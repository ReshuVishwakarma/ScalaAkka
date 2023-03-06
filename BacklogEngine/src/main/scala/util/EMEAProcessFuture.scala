package util
import code.Orchestrator.execCon
import code.RuleEngine.{cmnent, inme, logger}
import common.DataPersistHandler
import constant.{PropertiesLoader, UPPBEConstants}
import core.DataPersistUtil.{DataPersist, convertObjLSKUToLSKU, convertObjPartToPart, convertObjPoToPo, convertObjSOLToSOL, convertObjSOToSO, convertObjSoAttRefToSoAttRef}
import core.{CommonApiUtil, CommonDBUtil, CommonUtils}
import entities.{BacklogProcess, CommonEntity, ConfigVales, ObjSoAttRef}
import kafka.KafkaProducer
import net.liftweb.json.DefaultFormats
import net.liftweb.json.Serialization.write

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.util.{Failure, Success}

object EMEAProcessFuture {

  implicit val formats = DefaultFormats
  implicit val ec = execCon

  val cmnapiUtil = new CommonApiUtil()
  val cmndbUtil = new CommonDBUtil(cmnent, inme)
  val kafkaProducer = new KafkaProducer()
  val cmnUtil = new CommonUtils()
  val dataPersistUtil = new DataPersistHandler(cmnent)

  def ProcessEMEAOrders(incomingMessage: CommonEntity) : String =  {
    var flg = true
    try {
      logger.info("Inside EMEA Process for event id " + incomingMessage.event_ID )
      if (incomingMessage.event_TYPE=="WO" && incomingMessage.order_STATUS.toInt>=8000){
        logger.info("Got WO event for event id " + incomingMessage.event_ID )
        cmnapiUtil.insertBklgIm(incomingMessage.sales_ORDER_ID.toLong,incomingMessage.buid.toLong,incomingMessage.event_ID)
        if (cmnUtil.IS_3PL_LOGIC_ENABLED("EMEA_3PL")) {
          cmnapiUtil.thirdPartyLogic(incomingMessage.buid.toString, incomingMessage.event_ID.toString,
            incomingMessage.sales_ORDER_ID)
          cmnapiUtil.insertBacklog(incomingMessage.buid.toString, incomingMessage.event_ID.toString,
            incomingMessage.sales_ORDER_ID)
        }
        incomingMessage.event_ID.toString
      }
      val salesOrder = cmnapiUtil.getSalesOrder(PropertiesLoader.EMEASalesOrderUrl,
        incomingMessage.sales_ORDER_ID, incomingMessage.buid.toString, incomingMessage.event_ID)
      //logger.info("Completed getSalesOrder Api")
      logger.info("Completed getSalesOrder Api for event id " + incomingMessage.event_ID)
      if (salesOrder == null) {
        kafkaProducer.pushMessagetoRetry(incomingMessage, "No SalesRecord from API")
        flg = false // graceful stop process like above
      }
      else {

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
              logger.info("Completed EMEA backlog processing for event id " + incomingMessage.event_ID)
            }
            case Failure(e) =>
              logger.error("Failed during Delete Header and Detail for event id " + incomingMessage.event_ID, e.printStackTrace)
              kafkaProducer.pushMessagetoRetry(incomingMessage, "Failed During Delete Header and Detail")
          }(execCon)
        }

        else {
          val config = getConfig(salesOrder.sourceSystemStatusCode)
          val prodOrderApi = cmnapiUtil.getProdOrder(config.proOrderurl, salesOrder.salesOrderRef, salesOrder.buId.toString, salesOrder.region, incomingMessage.event_ID)

          if (prodOrderApi == null || prodOrderApi.size == 0) {
            kafkaProducer.pushMessagetoRetry(incomingMessage, "No ProdOrder Data")
            flg = false
          }
          if (flg) {
            val prodOrder = prodOrderApi.filter(c => (cmndbUtil.getfulfillmentChannel(salesOrder.region).contains(c.channelCode)))

            if (prodOrderApi == null || prodOrder.size == 0) {
              cmndbUtil.updateStatus(incomingMessage.event_ID, "NotQualified", "NA", "Channel Check failed", 0);
              flg = false
            }
            else if (!new CommonUtils().checkProdOrderHasCCNValues(prodOrder)) {
              kafkaProducer.pushMessagetoRetry(incomingMessage, "CCN Check failed")
              flg = false
            }
            if (flg) {

              val pof = Future[Boolean] {
                dataPersistUtil.writeDatatoTable(config.prodOrderTable, convertObjPoToPo(incomingMessage.event_ID, prodOrder))
              }(execCon)

              val salesOrderLineApi = cmnapiUtil.getSalesOrderLine(config.solurl, false, salesOrder.salesOrderRef, salesOrder.sourceSystemStatusCode, incomingMessage.event_ID)
              logger.info("Event_id- "+incomingMessage.event_ID+" || Completed getSalesOrderLine Api")
              if (salesOrderLineApi == null || salesOrderLineApi.size == 0) {
                kafkaProducer.pushMessagetoRetry(incomingMessage, "No SOLRecord from API")
                flg = false
              }
              if (flg) {
                val salesOrderLine = salesOrderLineApi.filter(c => !(ArrayBuffer[String]("603", "803").contains(c.mfgLob)))
                if (salesOrderLine.size == 0) {
                  cmndbUtil.updateStatus(incomingMessage.event_ID, "NotQualified", "NA", "MFG LOB not in check(603,803) failed", 0);
                  cmndbUtil.purgeInMemData(incomingMessage.event_ID)
                  flg = false
                }

                if (flg) {
                  if (new CommonUtils().isEMCMfgMethod(salesOrderLine)) {
                    cmndbUtil.updateStatus(incomingMessage.event_ID, "NotQualified", "NA", "IsEMCOrderCheckFailed", 0);
                    cmndbUtil.purgeInMemData(incomingMessage.event_ID)
                    flg = false
                  }

                  if (flg) {
                    if (new CommonUtils().hasFGA(prodOrder)) {
                      if (new CommonUtils().isGolfOTMEnabled(salesOrder.salesOrderRef, incomingMessage.event_ID)) {
                        cmndbUtil.updateStatus(incomingMessage.event_ID, "NotQualified", "NA", "hasFGACheckFailed", 0);
                        cmndbUtil.purgeInMemData(incomingMessage.event_ID)
                        flg = false
                      }
                    }
                    if (flg) {
                      val solf = Future[Boolean] {
                        dataPersistUtil.writeDatatoTable(config.solTable, convertObjSOLToSOL(incomingMessage.event_ID, salesOrderLine))
                      }(execCon)

                      val linesku = cmnapiUtil.getlskuData(config.lskuUrl, "{ \"lineSkuRef\":  " + write(salesOrderLine.map(_.soLineRef)) + "}", incomingMessage.event_ID)
                      logger.info("Event_id- "+incomingMessage.event_ID+" || Completed getlskuData Api")

                      if (linesku == null || linesku.size == 0) {
                        kafkaProducer.pushMessagetoRetry(incomingMessage, "No records in LineSKU")
                        flg = false
                      }
                      if (flg) {
                        val lskuf = Future[Boolean] {
                          dataPersistUtil.writeDatatoTable(config.lskuTable, convertObjLSKUToLSKU(incomingMessage.event_ID, linesku))
                        }(execCon)

                        val partsku = cmnapiUtil.getPartData(config.partUrl, "{ \"lineSkuRef\":  " + write(linesku.map(_.lineSkuRef)) + "}", incomingMessage.event_ID)
                        logger.info("Event_id- "+incomingMessage.event_ID+" || Completed getPartData Api")
                        if (partsku == null || partsku.size == 0) {
                          kafkaProducer.pushMessagetoRetry(incomingMessage, "No Part Data")
                        }

                        if (partsku != null && partsku.size > 0) {
                          val partf = Future[Boolean] {
                            dataPersistUtil.writeDatatoTable(config.partTable, convertObjPartToPart(incomingMessage.event_ID, partsku))
                          }(execCon)

                          val so_attr = cmnapiUtil.getSOAttrRef(config.soattrref, salesOrder.salesOrderRef,
                            salesOrder.region, incomingMessage.event_ID)
                          logger.info(" Completed getSOAttrRef API  for event id " + incomingMessage.event_ID)

                            val soat = check_so_attr_ref_datapersist(incomingMessage,so_attr)
                            val ls = List(sof, pof, solf, lskuf, partf, soat)
                            val futures = Future.sequence(ls)

                            futures.onComplete {
                              case Success(x) => {
                                logger.info("Completed All Futures" + x)
                                try {
                                  if (flg) {
                                    backlogProcess(config.backlog, incomingMessage)
                                    if (cmnUtil.IS_3PL_LOGIC_ENABLED("EMEA_3PL")) {
                                      cmnapiUtil.thirdPartyLogic(incomingMessage.buid.toString, incomingMessage.event_ID.toString,
                                        incomingMessage.sales_ORDER_ID)
                                    }
                                    cmnapiUtil.insertBacklog(incomingMessage.buid.toString, incomingMessage.event_ID.toString,
                                      incomingMessage.sales_ORDER_ID)
                                  }
                                }
                                catch {
                                  case ex => {
                                    logger.error("Exception During EMEA Backlog Process for event id " + incomingMessage.event_ID, ex)
                                    kafkaProducer.pushMessagetoRetry(incomingMessage, "Exception During EMEA Backlog Process")
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
      incomingMessage.event_ID.toString
    }
    catch{
      case ex =>
        logger.error("Exception during EMEA process for event id "+ incomingMessage.event_ID,ex)
        kafkaProducer.pushMessagetoRetry(incomingMessage, "Exception during EMEA process")
        incomingMessage.event_ID.toString
    }
  }

  def getConfig(status: String): ConfigVales = {
    if (status == "BK") {
      ConfigVales(PropertiesLoader.EMEA_ProdOrderOPRUrl, UPPBEConstants.PROD_ORDER_OPR_ENTITY,
        PropertiesLoader.EMEA_SOLOPRUrl, UPPBEConstants.SALES_ORDER_LINE_OPR_ENTITY,
        PropertiesLoader.EMEA_LineSKUOprUrl, UPPBEConstants.LINE_SKU_OPR_ENTITY,
        PropertiesLoader.EMEA_LineSKUPartOPRUrl, UPPBEConstants.PART_OPR_ENTITY,
        PropertiesLoader.EMEA_BacklogOPRUrl,PropertiesLoader.EMEA_SO_Attribute_Ref,"","")
    }
    else {
      ConfigVales(PropertiesLoader.EMEA_ProdOrderUrl, UPPBEConstants.PROD_ORDER_ENTITY,
        PropertiesLoader.EMEA_SOLUrl, UPPBEConstants.SALES_ORDER_LINE_ENTITY,
        PropertiesLoader.EMEA_LineSKUUrl, UPPBEConstants.LINE_SKU_ENTITY,
        PropertiesLoader.EMEA_LineSKUPartUrl, UPPBEConstants.PART_ENTITY,
        PropertiesLoader.EMEA_BacklogUrl,PropertiesLoader.EMEA_SO_Attribute_Ref,"","")
    }
  }

  def backlogProcess(url: String, incomingMessage: CommonEntity): Unit = {
    val dbhf = Future[Unit] { cmnapiUtil.deleteBacklogHeader(incomingMessage.sales_ORDER_ID, incomingMessage.buid)}(execCon)
    val dbdf = Future[Unit] { cmnapiUtil.deleteBacklogDetail(incomingMessage.sales_ORDER_ID, incomingMessage.buid)}(execCon)

    Future.sequence(List(dbhf, dbdf)).onComplete {
      case Success(x) => {
        logger.info("Delete Header and Detail Completed for event_id "+incomingMessage.event_ID.toString)
        cmnapiUtil.updateEmeaBacklog(url, incomingMessage.buid.toString, incomingMessage.event_ID.toString, incomingMessage.sales_ORDER_ID)
        logger.info("Completed EMEA backlog processing for event_id "+incomingMessage.event_ID.toString)
      }
      case Failure(e) =>
        logger.error("Failed during EMEA backlog process ", e.printStackTrace)
        kafkaProducer.pushMessagetoRetry(incomingMessage, "Failed during EMEA backlog process for event_id "+incomingMessage.event_ID.toString)
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