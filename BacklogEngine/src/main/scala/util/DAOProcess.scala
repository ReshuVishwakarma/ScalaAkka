package util

import java.util.{Calendar, Properties}
import akka.actor.SupervisorStrategy.Stop
import akka.actor.TypedActor.dispatcher
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.{AllForOneStrategy, Props, Terminated}
import code.Orchestrator.execCon
import code.RuleEngine.{cmnent, config, inme, logger}
import common.DataPersistHandler
import constant.{PropertiesLoader, UPPBEConstants}
import core.DataPersistUtil.{DataPersist, convertObjLSKUToLSKU, convertObjPOUToPOU, convertObjPartToPart, convertObjPoToPo, convertObjSOLToSOL, convertObjSOToSO, convertObjSoAttRefToSoAttRef}
import core.{CommonApiUtil, CommonDBUtil, CommonUtils}
import entities.{CommonEntity, ConfigVales}
import kafka.KafkaProducer
import net.liftweb.json.Serialization.write
import net.liftweb.json.DefaultFormats

import scala.concurrent.Future
import scala.util.{Failure, Success}

object DAOProcess {
  implicit val formats = DefaultFormats

  trait IDAOProcess
  case class ProcessDAOOrders(incomingMessage: CommonEntity) extends IDAOProcess

  val cmnapiUtil = new CommonApiUtil()
  val cmndbUtil = new CommonDBUtil(cmnent, inme)
  val kafkaProducer = new KafkaProducer()
  val cmnUtil = new CommonUtils()
  val dataPersistUtil = new DataPersistHandler(cmnent)

  //val logger: LoggingUtil = new LoggingUtil(ExperienceEnum.MTRC, "UPP_Backlog_Engine", "UPP_Backlog_Engine_KafkaReadWrite", "UPPBE", "UPPBE_ControlM_JobName", "1.0-SNAPSHOT", "9999999999", "Dev")

  def apply(): Behavior[IDAOProcess] =
    Behaviors.setup {
      context =>

        Behaviors.receiveMessage { msg =>
          val incomingMessage = msg.asInstanceOf[ProcessDAOOrders].incomingMessage
          try {
            val event_ID=incomingMessage.event_ID
            var flag = true
            logger.info("Event_id || " +event_ID+" "+"Inside DAO Process for event id " )

            val salesOrder = cmnapiUtil.getSalesOrder(PropertiesLoader.DAOSalesOrderUrl,
              incomingMessage.sales_ORDER_ID, incomingMessage.buid.toString, incomingMessage.event_ID)
            logger.info("Event_id || " +event_ID+" "+"Completed getSalesOrder Api")

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
                val dbhf = Future[Unit] { cmnapiUtil.deleteBacklogHeader(incomingMessage.sales_ORDER_ID, incomingMessage.buid)}
                val dbdf = Future[Unit] { cmnapiUtil.deleteBacklogDetail(incomingMessage.sales_ORDER_ID, incomingMessage.buid)}

                Future.sequence(List(dbhf, dbdf)).onComplete {
                  case Success(x) => {
                    logger.info("Event_id || " +event_ID+" "+"Delete Header and Detail Completed")
                    cmndbUtil.updateStatus(incomingMessage.event_ID, "Completed", "NA", "Status is " + salesOrder.sourceSystemStatusCode, 0)
                    logger.info("Event_id || " +event_ID+" "+"Completed APJ-OFS backlog processing")
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
                    cmndbUtil.updateStatus(incomingMessage.event_ID, "Failed", "", "Channel Check failed", 0);
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
                    logger.info("Event_id || " +event_ID+" "+"Completed getSalesOrderLine Api")

                    if (salesOrderLine == null || salesOrderLine.size == 0) {
                      kafkaProducer.pushMessagetoRetry(incomingMessage, "No SOLRecord from API")
                      flag = false
                    }

                    if (flag) {
                      if (new CommonUtils().hasFGA(prodOrder)) {
                        if (new CommonUtils().isSSCNotIn(salesOrderLine)) {
                          cmndbUtil.updateStatus(incomingMessage.event_ID, "Failed", "NA", "hasFGACheckFailed", 0);
                          flag = false
                        }
                      }
                      if (flag) {
                        if (new CommonUtils().isInvntoryTransfer(salesOrder.salesOrderRef, incomingMessage.event_ID)) {
                          cmndbUtil.updateStatus(incomingMessage.event_ID, "Failed", "NA", "HasInventoryTransfer", 0);
                          flag = false
                        }

                        if (flag) {
                          if (new CommonUtils().isEMCMfgMethod(salesOrderLine)) {
                            cmndbUtil.updateStatus(incomingMessage.event_ID, "Failed", "NA", "IsEMCMethodCheckFailed", 0);
                            flag = false
                          }

                          if (flag) {
                            if (new CommonUtils().is3PLRestrictionEnabled(salesOrder.salesOrderRef, incomingMessage.event_ID)) {
                              cmndbUtil.updateStatus(incomingMessage.event_ID, "Failed", "NA", "Is3PLRestrictionEnabledCheck failed", 0);
                              flag = false
                            }
                            if (flag) {
                              val solf = Future[Boolean] {
                                dataPersistUtil.writeDatatoTable(config.solTable, convertObjSOLToSOL(incomingMessage.event_ID, salesOrderLine))
                              }(execCon)

                              val linesku = cmnapiUtil.getlskuData(config.lskuUrl, "{ \"lineSkuRef\":  " + write(salesOrderLine.map(_.soLineRef)) + "}", incomingMessage.event_ID)
                              logger.info("Event_id || " +event_ID+" "+"Completed getlskuData Api")

                              if (linesku == null || linesku.size == 0) {
                                kafkaProducer.pushMessagetoRetry(incomingMessage, "No Linesku data")
                                flag = false
                              }
                              if (flag) {
                                val prodUnit = cmnapiUtil.getProdUnit(config.prodOrderUnitUrl, "{ \"soLineRef\":  " + write(salesOrderLine.map(_.soLineRef)) + "}", incomingMessage.event_ID)
                                logger.info("Event_id || " +event_ID+" "+"Completed getProdUnit Api")
                                if (prodUnit == null || prodUnit.size == 0) {
                                  logger.info("Event_id || " +event_ID+" "+"No Prod Order Unit")
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
                                logger.info("Event_id || " +event_ID+" "+"Completed getPartData Api")
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
                                  logger.info(" Completed getSOAttrRef API ")
                                  if (so_attr == null) {
                                    kafkaProducer.pushMessagetoRetry(incomingMessage, "No SO Attribute Ref from API")
                                    flag = false
                                  }

                                  if (flag) {
                                    val soat = Future[Boolean] {
                                      dataPersistUtil.writeDatatoTable(UPPBEConstants.SO_ATTRIBUTE_REF, convertObjSoAttRefToSoAttRef(incomingMessage.event_ID, so_attr))
                                    }(execCon)

                                    val ls = Seq(sof, pof, solf, lskuf, partf, soat)
                                    val futures = Future.sequence(ls)

                                    futures.onComplete {
                                      case Success(x) => {
                                        logger.info("Completed All Futures for event id " + incomingMessage.event_ID + x)
                                        try {
                                          if (flag) {
                                            backlogProcess(config.backlog, incomingMessage)
                                          }
                                        }
                                        catch {
                                          case ex => {
                                            logger.error("Exception During DAO Backlog Process for event id " + incomingMessage.event_ID, ex)
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
            }
            Behaviors.same
          }
          catch {
            case ex  =>
              logger.error("Exception During DAO Process")
              kafkaProducer.pushMessagetoRetry(incomingMessage, "Exception in DAO Process")
              Behaviors.same
          }
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

    val dbhf = Future[Unit] { cmnapiUtil.deleteBacklogHeader(incomingMessage.sales_ORDER_ID, incomingMessage.buid) }
    val dbdf = Future[Unit] { cmnapiUtil.deleteBacklogDetail(incomingMessage.sales_ORDER_ID, incomingMessage.buid) }

    Future.sequence(List(dbhf, dbdf)).onComplete {
      case Success(x) => {
        logger.info("Event_id || " +incomingMessage.event_ID+" "+"Delete Header and Detail Completed")
        cmnapiUtil.updateBacklogDao(url, incomingMessage.buid.toString, incomingMessage.event_ID.toString, incomingMessage.sales_ORDER_ID)
        logger.info("Event_id || " +incomingMessage.event_ID+" "+"Completed DAO backlog processing")
      }
      case Failure(e) =>
        logger.error("Failed during DAO backlog process ", e.printStackTrace)
        kafkaProducer.pushMessagetoRetry(incomingMessage, "Failed during DAO backlog process ")
    }(execCon)
  }
}