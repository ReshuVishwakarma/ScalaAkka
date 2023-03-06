package util

import code.Orchestrator.execCon
import code.RuleEngine.{cmnent, config, inme, logger}
import common.DataPersistHandler
import constant.{PropertiesLoader, UPPBEConstants}
import core.DataPersistUtil.{DataPersist, convertObjLSKUToLSKU, convertObjPOUToPOU, convertObjPartToPart, convertObjPoToPo, convertObjSOLToSOL, convertObjSOToSO, convertObjSoAttRefToSoAttRef}
import core.{CommonApiUtil, CommonDBUtil, CommonUtils}
import entities.{CommonEntity, ConfigVales, ObjSoAttRef}
import kafka.KafkaProducer
import net.liftweb.json.Serialization.write
import net.liftweb.json.DefaultFormats

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.util.{Failure, Success}

object DAOProcessFuture {
  implicit val formats = DefaultFormats
  implicit val ec = execCon

  val cmnapiUtil = new CommonApiUtil()
  val cmndbUtil = new CommonDBUtil(cmnent, inme)
  val kafkaProducer = new KafkaProducer()
  val cmnUtil = new CommonUtils()
  val dataPersistUtil = new DataPersistHandler(cmnent)

  //val logger: LoggingUtil = new LoggingUtil(ExperienceEnum.MTRC, "UPP_Backlog_Engine", "UPP_Backlog_Engine_KafkaReadWrite", "UPPBE", "UPPBE_ControlM_JobName", "1.0-SNAPSHOT", "9999999999", "Dev")
  def ProcessDAOOrders(incomingMessage: CommonEntity)  : String = {
    try {
      var flag = true
      logger.info("Inside DAO Process for event id " + incomingMessage.event_ID)
      if (incomingMessage.event_TYPE=="WO" && incomingMessage.order_STATUS.toInt>=8000){
        logger.info("Got WO event for event id " + incomingMessage.event_ID )
        cmnapiUtil.insertBklgIm(incomingMessage.sales_ORDER_ID.toLong,incomingMessage.buid.toLong,incomingMessage.event_ID)
        if (cmnUtil.IS_3PL_LOGIC_ENABLED("DAO_3PL")) {
          cmnapiUtil.thirdPartyLogic(incomingMessage.buid.toString, incomingMessage.event_ID.toString,
            incomingMessage.sales_ORDER_ID)
          cmnapiUtil.insertBacklog(incomingMessage.buid.toString, incomingMessage.event_ID.toString,
            incomingMessage.sales_ORDER_ID)
        }
        incomingMessage.event_ID.toString
      }
      val salesOrder = cmnapiUtil.getSalesOrder(PropertiesLoader.DAOSalesOrderUrl,
        incomingMessage.sales_ORDER_ID, incomingMessage.buid.toString, incomingMessage.event_ID)
      logger.info("Completed getSalesOrder Api for event id " + incomingMessage.event_ID)

      if (salesOrder == null) {
        kafkaProducer.pushMessagetoRetry(incomingMessage, "No SalesRecord from API")
        flag = false
      }

      if (flag) {
        val config = getConfig(salesOrder.sourceSystemStatusCode)
        val sof = Future[Boolean] {
          dataPersistUtil.writeDatatoTable(UPPBEConstants.SALES_ORDER_ENTITY, convertObjSOToSO(incomingMessage.event_ID, salesOrder))
        }(execCon)

        if (cmndbUtil.status_chk_for_purge(salesOrder.sourceSystemStatusCode, salesOrder.region).equalsIgnoreCase(salesOrder.sourceSystemStatusCode)) {
          val dbhf = Future[Unit] {
            cmnapiUtil.deleteBacklogHeader(incomingMessage.sales_ORDER_ID, incomingMessage.buid)
          }(execCon)
          val dbdf = Future[Unit] {
            cmnapiUtil.deleteBacklogDetail(incomingMessage.sales_ORDER_ID, incomingMessage.buid)
          }(execCon)

          Future.sequence(List(dbhf, dbdf)).onComplete {
            case Success(x) => {
              logger.info("Delete Header and Detail Completed for event id " + incomingMessage.event_ID)
              cmndbUtil.updateStatus(incomingMessage.event_ID, "Completed", "NA", "Status is " + salesOrder.sourceSystemStatusCode, 0)
              logger.info("Completed APJ-OFS backlog processing")
            }
            case Failure(e) =>
              logger.error("Failed during Delete Header and Detail", e.printStackTrace)
              kafkaProducer.pushMessagetoRetry(incomingMessage, "Failed During Delete Header and Detail")
          }(execCon)
        }
        else {

          val prodOrderApi = cmnapiUtil.getProdOrder(config.proOrderurl, salesOrder.salesOrderRef, salesOrder.buId.toString, salesOrder.region, incomingMessage.event_ID)

          if (prodOrderApi == null || prodOrderApi.size == 0) {
            kafkaProducer.pushMessagetoRetry(incomingMessage, "No ProdOrder Data")
            flag = false
          }
          if (flag) {
            val prodOrder = prodOrderApi.filter(c => (cmndbUtil.getfulfillmentChannel(salesOrder.region).contains(c.channelCode)))

            if (prodOrder == null || prodOrder.size == 0) {
              cmndbUtil.updateStatus(incomingMessage.event_ID, "NotQualified", "", "Channel Check failed", 0);
              flag = false
            }
            else if (!new CommonUtils().checkProdOrderHasCCNValues(prodOrder)) {
              kafkaProducer.pushMessagetoRetry(incomingMessage, "CCN Check failed")
              flag = false
            }
            if (flag) {

              val pof = Future[Boolean] {
                dataPersistUtil.writeDatatoTable(config.prodOrderTable, convertObjPoToPo(incomingMessage.event_ID, prodOrder))
              }(execCon)

              val salesOrderLine = cmnapiUtil.getSalesOrderLine(config.solurl, false, salesOrder.salesOrderRef, salesOrder.sourceSystemStatusCode, incomingMessage.event_ID)
              logger.info("Completed getSalesOrderLine Api for event id " + incomingMessage.event_ID)

              if (salesOrderLine == null || salesOrderLine.size == 0) {
                kafkaProducer.pushMessagetoRetry(incomingMessage, "No SOLRecord from API")
                flag = false
              }

              if (flag) {
                if (new CommonUtils().hasFGA(prodOrder)) {
                  if (new CommonUtils().isSSCNotIn(salesOrderLine)) {
                    cmndbUtil.updateStatus(incomingMessage.event_ID, "NotQualified", "NA", "hasFGACheckFailed", 0);
                    flag = false
                  }
                }
                if (flag) {
                  if (new CommonUtils().isInvntoryTransfer(salesOrder.salesOrderRef, incomingMessage.event_ID)) {
                    cmndbUtil.updateStatus(incomingMessage.event_ID, "NotQualified", "NA", "HasInventoryTransfer", 0);
                    flag = false
                  }

                  if (flag) {
                    if (new CommonUtils().isEMCMfgMethod(salesOrderLine)) {
                      cmndbUtil.updateStatus(incomingMessage.event_ID, "NotQualified", "NA", "IsEMCMethodCheckFailed", 0);
                      flag = false
                    }

                    if (flag) {
                      if (new CommonUtils().is3PLRestrictionEnabled(salesOrder.salesOrderRef, incomingMessage.event_ID)) {
                        cmndbUtil.updateStatus(incomingMessage.event_ID, "NotQualified", "NA", "Is3PLRestrictionEnabledCheck failed", 0);
                        flag = false
                      }
                      if (flag) {
                        val solf = Future[Boolean] {
                          dataPersistUtil.writeDatatoTable(config.solTable, convertObjSOLToSOL(incomingMessage.event_ID, salesOrderLine))
                        }(execCon)

                        val linesku = cmnapiUtil.getlskuData(config.lskuUrl, "{ \"lineSkuRef\":  " + write(salesOrderLine.map(_.soLineRef)) + "}", incomingMessage.event_ID)
                        logger.info("Completed getlskuData Api for event id " + incomingMessage.event_ID)

                        if (linesku == null || linesku.size == 0) {
                          kafkaProducer.pushMessagetoRetry(incomingMessage, "No Linesku data")
                          flag = false
                        }
                        if (flag) {
                          val prodUnit = cmnapiUtil.getProdUnit(config.prodOrderUnitUrl, "{ \"soLineRef\":  " + write(salesOrderLine.map(_.soLineRef)) + "}", incomingMessage.event_ID)
                          logger.info("Completed getProdUnit Api for event id " + incomingMessage.event_ID)
                          if (prodUnit == null || prodUnit.size == 0) {
                            logger.info("No Prod Order Unit")
                          }
                          else {
                            val pouf = Future[Boolean] {
                              dataPersistUtil.writeDatatoTable(config.prodOrderUnitUrl, convertObjPOUToPOU(incomingMessage.event_ID, prodUnit))
                            }(execCon)
                          }

                          val lskuf = Future[Boolean] {
                            dataPersistUtil.writeDatatoTable(config.lskuTable, convertObjLSKUToLSKU(incomingMessage.event_ID, linesku))
                          }(execCon)

                          val partsku = cmnapiUtil.getPartData(config.partUrl, "{ \"lineSkuRef\":  " + write(linesku.map(_.lineSkuRef)) + "}", incomingMessage.event_ID)
                          logger.info("Completed getPartData Api for event id " + incomingMessage.event_ID)
                          if (partsku == null || partsku.size == 0) {
                            kafkaProducer.pushMessagetoRetry(incomingMessage, "No Part Data")
                            flag = false
                          }
                          else {
                            val partf = Future[Boolean] {
                              dataPersistUtil.writeDatatoTable(config.partTable, convertObjPartToPart(incomingMessage.event_ID, partsku))
                            }(execCon)

                            val so_attr = cmnapiUtil.getSOAttrRef(config.soattrref, salesOrder.salesOrderRef,
                              salesOrder.region, incomingMessage.event_ID)
                            logger.info(" Completed getSOAttrRef API  for event id " + incomingMessage.event_ID)

                            val soat = check_so_attr_ref_datapersist(incomingMessage,so_attr)
                              val ls = Seq(sof, pof, solf, lskuf, partf, soat)
                              val futures = Future.sequence(ls)

                              futures.onComplete {
                                case Success(x) => {
                                  logger.info("Completed All Futures for event id " + incomingMessage.event_ID + x)
                                  try {
                                    if (flag) {
                                      backlogProcess(config.backlog, incomingMessage)
                                      if (cmnUtil.IS_3PL_LOGIC_ENABLED("DAO_3PL")) {
                                        cmnapiUtil.thirdPartyLogic(incomingMessage.buid.toString, incomingMessage.event_ID.toString,
                                          incomingMessage.sales_ORDER_ID)

                                      }
                                      cmnapiUtil.insertBacklog(incomingMessage.buid.toString, incomingMessage.event_ID.toString,
                                        incomingMessage.sales_ORDER_ID)
                                    }
                                  }
                                  catch {
                                    case ex => {
                                      logger.error("Exception During DAO Backlog Process for event id " + incomingMessage.event_ID, ex)
                                      kafkaProducer.pushMessagetoRetry(incomingMessage, "Exception During DAO Backlog Process")
                                    }
                                  }
                                }
                                case Failure(e) =>
                                  logger.error("Failed During Persist for event id " + incomingMessage.event_ID, e.printStackTrace)
                                  kafkaProducer.pushMessagetoRetry(incomingMessage, "Failed During Persist")
                              }(execCon)

                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
      incomingMessage.event_ID.toString
    }
    catch {
      case ex =>
        logger.error("Exception During DAO Process for event id " + incomingMessage.event_ID)
        kafkaProducer.pushMessagetoRetry(incomingMessage, "Exception in DAO Process")
        incomingMessage.event_ID.toString
    }
  }


  def getConfig(status: String): ConfigVales = {

    if (status == "BK") {
      ConfigVales(PropertiesLoader.DAO_ProdOrderOPRUrl, UPPBEConstants.PROD_ORDER_OPR_ENTITY,
        PropertiesLoader.DAO_SOLOPRUrl, UPPBEConstants.SALES_ORDER_LINE_OPR_ENTITY,
        PropertiesLoader.DAO_LineSKUOprUrl, UPPBEConstants.LINE_SKU_OPR_ENTITY,
        PropertiesLoader.DAO_LineSKUPartOPRUrl, UPPBEConstants.PART_OPR_ENTITY,
        PropertiesLoader.DAO_backlogOPRUrl,PropertiesLoader.DAO_SO_Attribute_Ref, PropertiesLoader.DAO_ProdOrderUnitUrl,
        UPPBEConstants.GET_PROD_ORDER_UNIT)
    }
    else {
      ConfigVales(PropertiesLoader.DAO_ProdOrderUrl, UPPBEConstants.PROD_ORDER_ENTITY,
        PropertiesLoader.DAO_SOLUrl, UPPBEConstants.SALES_ORDER_LINE_ENTITY,
        PropertiesLoader.DAO_LineSKUUrl, UPPBEConstants.LINE_SKU_ENTITY,
        PropertiesLoader.DAO_LineSKUPartUrl, UPPBEConstants.PART_ENTITY,
        PropertiesLoader.DAO_backlogOFSUrl,PropertiesLoader.DAO_SO_Attribute_Ref, PropertiesLoader.DAO_ProdOrderUnitUrl,
        UPPBEConstants.GET_PROD_ORDER_UNIT)
    }
  }

  def backlogProcess(url: String, incomingMessage: CommonEntity): Unit = {

    val dbhf = Future[Unit] { cmnapiUtil.deleteBacklogHeader(incomingMessage.sales_ORDER_ID, incomingMessage.buid) }(execCon)
    val dbdf = Future[Unit] { cmnapiUtil.deleteBacklogDetail(incomingMessage.sales_ORDER_ID, incomingMessage.buid) }(execCon)

    Future.sequence(List(dbhf, dbdf)).onComplete {
      case Success(x) => {
        logger.info("Delete Header and Detail Completed for event id " + incomingMessage.event_ID)
        cmnapiUtil.updateBacklogDao(url, incomingMessage.buid.toString, incomingMessage.event_ID.toString, incomingMessage.sales_ORDER_ID)
        logger.info("Completed DAO backlog processing for event id " + incomingMessage.event_ID)
      }
      case Failure(e) =>
        logger.error("Event_id- "+incomingMessage.event_ID.toString+" || Failed during DAO backlog process ", e.printStackTrace)
        kafkaProducer.pushMessagetoRetry(incomingMessage, "Failed during DAO backlog process ")
    }(execCon)
  }
  def check_so_attr_ref_datapersist(incomingMessage:CommonEntity,so_attr:ArrayBuffer[ObjSoAttRef]): Future[Boolean]  ={
    if (so_attr == null || so_attr.size == 0){
      var soat = Future[Boolean]{true}(execCon)
      soat
    }
    else  {
      var soat = Future[Boolean] {
        dataPersistUtil.writeDatatoTable(UPPBEConstants.SO_ATTRIBUTE_REF, convertObjSoAttRefToSoAttRef(incomingMessage.event_ID, so_attr))
      }(execCon)
      soat
    }
  }
}