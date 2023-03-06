package util

import code.Orchestrator.execCon
import code.RuleEngine.{cmnent, config, inme, logger}
import common.DataPersistHandler
import constant.PropertiesLoader
import constant.UPPBEConstants
import core.DataPersistUtil.{DataPersist, convertObjDDToDD, convertObjDFToDF, convertObjDHToDH, convertObjLSKUToLSKU, convertObjPartToPart, convertObjPoToPo, convertObjSOLToSOL, convertObjSOToSO, convertObjSoAttRefToSoAttRef, dataPersistUtil}
import core.{CommonApiUtil, CommonDBUtil, CommonUtils}
import entities.{CommonEntity, ConfigVales, ObjLSKU, ObjPart, ObjProdOrder, ObjSalesOrder, ObjSalesOrderLine, ObjSoAttRef}
import kafka.KafkaProducer
import net.liftweb.json._
import net.liftweb.json.Serialization.write

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.reflect.internal.util.TriState.False
import scala.util.{Failure, Success}

object APJProcessFuture {

  implicit val formats = DefaultFormats
  implicit val ec = execCon

  val cmnapiUtil = new CommonApiUtil()
  val cmndbUtil = new CommonDBUtil(cmnent, inme)
  val kafkaProducer = new KafkaProducer()
  val cmnUtil = new CommonUtils()
  val dataPersistUtil = new DataPersistHandler(cmnent)

  def ProcessAPJOrderFuture(incomingMessage: CommonEntity) : String = {
    var flg = true
    var so_atri_flag = true
    try {
      logger.info("Inside APJ Process for event id " + incomingMessage.event_ID)
      if (incomingMessage.event_TYPE=="WO"){
        logger.info("Got WO event for event id " + incomingMessage.event_ID)
        cmnapiUtil.insertBklgIm(incomingMessage.sales_ORDER_ID.toLong,incomingMessage.buid.toLong,incomingMessage.event_ID)
        if (cmnUtil.IS_3PL_LOGIC_ENABLED("APJ_3PL") && incomingMessage.order_STATUS.toInt>=8000) {
          cmnapiUtil.thirdPartyLogic(incomingMessage.buid.toString, incomingMessage.event_ID.toString,
            incomingMessage.sales_ORDER_ID)
          cmnapiUtil.insertBacklog(incomingMessage.buid.toString, incomingMessage.event_ID.toString,
            incomingMessage.sales_ORDER_ID)
        }
        incomingMessage.event_ID.toString
      }
      val salesOrder = cmnapiUtil.getSalesOrder(PropertiesLoader.APJSalesOrderUrl,
        incomingMessage.sales_ORDER_ID, incomingMessage.buid.toString, incomingMessage.event_ID)
      logger.info(" Completed getSalesOrder API  for event id " + incomingMessage.event_ID)
      if (salesOrder == null) {
        kafkaProducer.pushMessagetoRetry(incomingMessage, "No SalesRecord from API")
        flg = false
      }

      if (flg) {
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
              logger.info("Completed APJ-OFS backlog processing for event id " + incomingMessage.event_ID)
            }
            case Failure(e) =>
              logger.error("Event_id- "+incomingMessage.event_ID.toString+" || Failed during Delete Header and Detail ", e.printStackTrace)
              kafkaProducer.pushMessagetoRetry(incomingMessage, "Failed During Delete Header and Detail")
          }(execCon)
        }
        else {
          val config = getConfig(salesOrder.sourceSystemStatusCode)

          val prodOrderApi = cmnapiUtil.getProdOrder(config.proOrderurl, salesOrder.salesOrderRef, salesOrder.buId.toString,
            salesOrder.region, incomingMessage.event_ID)
          logger.info(" Completed getProdOrder API  for event id " + incomingMessage.event_ID)

          if (prodOrderApi == null || prodOrderApi.size == 0) {
            kafkaProducer.pushMessagetoRetry(incomingMessage, "No ProdOrder Data")
            flg = false
          }

          if (flg) {
            val prodOrder = prodOrderApi.filter(c => (cmndbUtil.getfulfillmentChannel(salesOrder.region).contains(c.channelCode)))
            if (prodOrder == null || prodOrder.size == 0) {
              cmndbUtil.updateStatus(incomingMessage.event_ID, "NotQualified", "NA", "Channel Check failed", 0)
              flg = false
            }
            else if (!cmnUtil.checkProdOrderHasCCNValues(prodOrder)) {
              kafkaProducer.pushMessagetoRetry(incomingMessage, "CCN Check failed")
              flg = false
            }

            if (flg) {
              val pof = Future[Boolean] {
                dataPersistUtil.writeDatatoTable(config.prodOrderTable, convertObjPoToPo(incomingMessage.event_ID, prodOrder))
              }(execCon)

              val salesOrderLine = cmnapiUtil.getSalesOrderLine(config.solurl, false, salesOrder.salesOrderRef,
                salesOrder.sourceSystemStatusCode, incomingMessage.event_ID)
              logger.info(" Completed getSalesOrderLine API  for event id " + incomingMessage.event_ID)

              if (salesOrderLine == null || salesOrderLine.size == 0) {
                kafkaProducer.pushMessagetoRetry(incomingMessage, "No SOLRecord from API")
                flg = false
              }

              if (flg) {
                val solf = Future[Boolean] {
                  dataPersistUtil.writeDatatoTable(config.solTable, convertObjSOLToSOL(incomingMessage.event_ID, salesOrderLine))
                }(execCon)

                val linesku = cmnapiUtil.getlskuData(config.lskuUrl, "{ \"lineSkuRef\":  " + write(salesOrderLine.map(_.soLineRef)) + "}", incomingMessage.event_ID)
                logger.info(" Completed getlsku Data API  for event id " + incomingMessage.event_ID)
                if (linesku == null || linesku.size == 0) {
                  kafkaProducer.pushMessagetoRetry(incomingMessage, "No LineSku from API")
                  flg = false
                }
                if (flg) {
                  val lskuf = Future[Boolean] {
                    dataPersistUtil.writeDatatoTable(config.lskuTable, convertObjLSKUToLSKU(incomingMessage.event_ID, linesku))
                  }(execCon)

                  val partsku = cmnapiUtil.getPartData(config.partUrl, "{ \"lineSkuRef\":  " + write(linesku.map("" + _.lineSkuRef + "")) + "}", incomingMessage.event_ID)
                  logger.info(" Completed partsku Data API  for event id " + incomingMessage.event_ID)
                  if (partsku == null || partsku.size == 0) {
                    kafkaProducer.pushMessagetoRetry(incomingMessage, "No Part Data")
                    flg = false
                  }
                  if (flg) {
                    val partf = Future[Boolean] {
                      dataPersistUtil.writeDatatoTable(config.partTable, convertObjPartToPart(incomingMessage.event_ID, partsku))
                    }(execCon)

                    val so_attr = cmnapiUtil.getSOAttrRef(config.soattrref, salesOrder.salesOrderRef,
                      salesOrder.region, incomingMessage.event_ID)
                    logger.info(" Completed getSOAttrRef API  for event id " + incomingMessage.event_ID)

                    val soat = check_so_attr_ref_datapersist(incomingMessage,so_attr)

                      if (salesOrder.sourceSystemStatusCode == "BK") {
                        flg = applyOFSOPRChecks(prodOrder, salesOrderLine, incomingMessage, salesOrder.salesOrderRef)
                        val ls = List(sof, pof, solf, lskuf, partf, soat)
                        val futures = Future.sequence(ls)

                        futures.onComplete {
                          case Success(x) => {
                            logger.info("Completed All Futures" + x)
                            try {
                              if (flg) {
                                backlogProcessOPR(config.backlog, incomingMessage, salesOrder.salesOrderRef)
                                if (cmnUtil.IS_3PL_LOGIC_ENABLED("APJ_3PL")) {
                                  cmnapiUtil.thirdPartyLogic(incomingMessage.buid.toString, incomingMessage.event_ID.toString,
                                    incomingMessage.sales_ORDER_ID)
                                }
                                cmnapiUtil.insertBacklog(incomingMessage.buid.toString, incomingMessage.event_ID.toString,
                                  incomingMessage.sales_ORDER_ID)
                              }
                            }
                            catch {
                              case ex => {
                                logger.error("Backlog Processing API throws an exception for event id " + incomingMessage.event_ID, ex)
                                kafkaProducer.pushMessagetoRetry(incomingMessage, "Backlog Processing API throws an exception")
                              }
                            } finally {
                              logger.info("Backlog API call ends for event id " + incomingMessage.event_ID)
                            }
                          }
                          case Failure(e) =>
                            logger.error("Failed During Persist for event id " + incomingMessage.event_ID, e.printStackTrace)
                            kafkaProducer.pushMessagetoRetry(incomingMessage, "Failed During Persist")
                        }(execCon)
                      }
                      else if (flg) {
                        if (flg) {
                          logger.info("------ OFS Order------")
                          val golfCount = cmnUtil.getGolfCount(salesOrder.salesOrderRef, incomingMessage.event_ID)
                          //List of Dragon Prod Order Number and OFS Prod Order Number
                          var OFS_ProdOrdNum: ArrayBuffer[ObjProdOrder] = new ArrayBuffer[ObjProdOrder]
                          var Drag_ProdOrdNum: ArrayBuffer[ObjProdOrder] = new ArrayBuffer[ObjProdOrder]
                          OFS_ProdOrdNum = prodOrder.filter(c => (c.statusCode.contains("HOLD") && c.channelCode.contains("DRAGON")) ||
                            ((c.channelCode.contains("FOOE") || c.channelCode.contains("O2")) && c.subChannel != null && !c.subChannel.contains(Some(None))) ||
                            (golfCount > 0 && (c.channelCode.contains("FOOE") || c.channelCode.contains("O2"))
                              && (c.subChannel == null || c.subChannel.contains(Some(None)))))

                          Drag_ProdOrdNum = prodOrder.filter(c => (!c.statusCode.contains("HOLD") &&
                            c.channelCode.contains("DRAGON")))

                          logger.info("OFSProdOrder separate list " + OFS_ProdOrdNum + " for event id " + incomingMessage.event_ID)
                          logger.info("Drag_ProdOrder separate list " + Drag_ProdOrdNum + " for event id " + incomingMessage.event_ID)

                          if (OFS_ProdOrdNum.size > 0 || Drag_ProdOrdNum.size > 0) {

                            val DragOrderFul = cmnapiUtil.getDragOrderFul(PropertiesLoader.Drag_ord_ful_url, incomingMessage.buid.toString, incomingMessage.sales_ORDER_ID, incomingMessage.event_ID)
                            val drgff = Future[Boolean] {
                              dataPersistUtil.writeDatatoTable(UPPBEConstants.DRGN_ORDER_FULFILLMENT_ENTITY, convertObjDFToDF(incomingMessage.event_ID, DragOrderFul))
                            }(execCon)

                            val DragOrderHeader = cmnapiUtil.getDragOrderHeader(PropertiesLoader.Drag_ord_header_url, incomingMessage.buid.toString, incomingMessage.sales_ORDER_ID, incomingMessage.event_ID)
                            val drghf = Future[Boolean] {
                              dataPersistUtil.writeDatatoTable(UPPBEConstants.DRGN_ORDER_HEADER_ENTITY, convertObjDHToDH(incomingMessage.event_ID, DragOrderHeader))
                            }(execCon)

                            val DragOrderDetail = cmnapiUtil.getDragOrderDetail(PropertiesLoader.Drag_ord_detail_url, incomingMessage.buid.toString, incomingMessage.sales_ORDER_ID, incomingMessage.event_TYPE.equalsIgnoreCase("DO"), incomingMessage.event_ID)
                            val drgdf = Future[Boolean] {
                              dataPersistUtil.writeDatatoTable(UPPBEConstants.DRGN_ORDER_DETAIL_ENTITY, convertObjDDToDD(incomingMessage.event_ID, DragOrderDetail))
                            }(execCon)

                            flg = applyOFSOPRChecks(prodOrder, salesOrderLine, incomingMessage, salesOrder.salesOrderRef)
                            val ls = List(sof, pof, solf, lskuf, partf, soat, drgff, drghf, drgdf)

                            val futures = Future.sequence(ls)
                            futures.onComplete {
                              case Success(x) => {
                                logger.info("Completed All Futures" + x)
                                try {
                                  val dbhf = Future[Unit] {
                                    cmnapiUtil.deleteBacklogHeader(incomingMessage.sales_ORDER_ID, incomingMessage.buid)
                                  }(execCon)
                                  val dbdf = Future[Unit] {
                                    cmnapiUtil.deleteBacklogDetail(incomingMessage.sales_ORDER_ID, incomingMessage.buid)
                                  }(execCon)

                                  Future.sequence(List(dbhf, dbdf)).onComplete {
                                    case Success(x) => {
                                      logger.info("Delete Header and Detail Completed for event id " + incomingMessage.event_ID)

                                      if (OFS_ProdOrdNum.size > 0 && flg) {
                                        cmnapiUtil.updateBacklogOFS(config.backlog, incomingMessage.buid.toString, incomingMessage.event_ID.toString, incomingMessage.sales_ORDER_ID, "\"prodOrderNum\":" + write(OFS_ProdOrdNum.map(_.prodOrderNum.toLong)))
                                        if (cmnUtil.IS_3PL_LOGIC_ENABLED("APJ_3PL")) {
                                          cmnapiUtil.thirdPartyLogic(incomingMessage.buid.toString, incomingMessage.event_ID.toString,
                                            incomingMessage.sales_ORDER_ID)
                                        }
                                        cmnapiUtil.insertBacklog(incomingMessage.buid.toString, incomingMessage.event_ID.toString,
                                          incomingMessage.sales_ORDER_ID)
                                        logger.info("Completed APJ-OFS backlog processing")
                                      }

                                      if (Drag_ProdOrdNum.size > 0 && flg) {
                                        cmnapiUtil.drgnBacklog(PropertiesLoader.Drag_bklg_url, incomingMessage.buid.toString, "DRAGON", incomingMessage.event_ID.toString, incomingMessage.sales_ORDER_ID)
                                        if (cmnUtil.IS_3PL_LOGIC_ENABLED("APJ_3PL")) {
                                          cmnapiUtil.thirdPartyLogic(incomingMessage.buid.toString, incomingMessage.event_ID.toString,
                                            incomingMessage.sales_ORDER_ID)
                                        }
                                        cmnapiUtil.insertBacklog(incomingMessage.buid.toString, incomingMessage.event_ID.toString,
                                          incomingMessage.sales_ORDER_ID)
                                        logger.info("Completed Dragon backlog processing")
                                      }
                                    }
                                    case Failure(e) =>
                                      logger.error("Event_id- "+incomingMessage.event_ID.toString+" || Failed Delete Header and Detail API", e.printStackTrace)
                                      kafkaProducer.pushMessagetoRetry(incomingMessage, "Failed During Delete Header and Detail")
                                  }(execCon)
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
                          else {
                            cmndbUtil.updateStatus(incomingMessage.event_ID, "NotQualified", "NA", "OFS_ProdOrdNum count " + OFS_ProdOrdNum.size + " and Drag_ProdOrdNum count " + Drag_ProdOrdNum.size, 0)
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
        logger.error("Exception During APJ process for event id " + incomingMessage.event_ID, ex)
        kafkaProducer.pushMessagetoRetry(incomingMessage, "Exception During APJ process")
        incomingMessage.event_ID.toString
    }
  }

  def applyOFSOPRChecks(prodOrder: ArrayBuffer[ObjProdOrder],
                        salesOrderLine: ArrayBuffer[ObjSalesOrderLine], incomingMessage: CommonEntity, soRef: String): Boolean = {
    var flg = true

    if (cmnUtil.isEMCOrder(prodOrder)) {
      if (cmnUtil.isEMCMfgMethod(salesOrderLine)) {
        cmndbUtil.updateStatus(incomingMessage.event_ID, "NotQualified", "NA", "IsEMCOrderCheckFailed", 0)
        flg = false
      }
    }
    if (flg) {
      logger.info("hasFGA(prodOrder) check")
      if (cmnUtil.hasFGA(prodOrder)) {
        if (cmnUtil.isGolfOTMEnabled(soRef, incomingMessage.event_ID)) {
          cmndbUtil.updateStatus(incomingMessage.event_ID, "NotQualified", "NA", "hasFGACheckFailed", 0)
          flg = false
        }
      }
    }
    flg
  }

  def getConfig(status: String): ConfigVales = {
    if (status == "BK") {
      ConfigVales(PropertiesLoader.APJ_ProdOrderOPRUrl, UPPBEConstants.PROD_ORDER_OPR_ENTITY,
        PropertiesLoader.APJ_SOLOPRUrl, UPPBEConstants.SALES_ORDER_LINE_OPR_ENTITY,
        PropertiesLoader.APJ_LineSKUOprUrl, UPPBEConstants.LINE_SKU_OPR_ENTITY,
        PropertiesLoader.APJ_LineSKUPartOPRUrl, UPPBEConstants.PART_OPR_ENTITY,
        PropertiesLoader.APJ_BacklogOPRUrl,PropertiesLoader.APJ_SO_Attribute_Ref, "", "")
    }
    else {
      ConfigVales(PropertiesLoader.APJ_ProdOrderUrl, UPPBEConstants.PROD_ORDER_ENTITY,
        PropertiesLoader.APJ_SOLUrl, UPPBEConstants.SALES_ORDER_LINE_ENTITY,
        PropertiesLoader.APJ_LineSKUUrl, UPPBEConstants.LINE_SKU_ENTITY,
        PropertiesLoader.APJ_LineSKUPartUrl, UPPBEConstants.PART_ENTITY,
        PropertiesLoader.APJ_BacklogUrl,PropertiesLoader.APJ_SO_Attribute_Ref, "", "")
    }
  }

/*  def backlogProcessOFS(url: String, incomingMessage: CommonEntity, OFS_ProdOrdNum: ArrayBuffer[ObjProdOrder]): Unit = {
    val dbhf = Future[Unit] {
      cmnapiUtil.deleteBacklogHeader(incomingMessage.sales_ORDER_ID, incomingMessage.buid)
    }(execCon)
    val dbdf = Future[Unit] {
      cmnapiUtil.deleteBacklogDetail(incomingMessage.sales_ORDER_ID, incomingMessage.buid)
    }(execCon)

    Future.sequence(List(dbhf, dbdf)).onComplete {
      case Success(x) => {
        logger.info("Delete Header and Detail Completed")
        cmnapiUtil.updateBacklogOFS(url, incomingMessage.buid.toString, incomingMessage.event_ID.toString, incomingMessage.sales_ORDER_ID, "\"prodOrderNum\":" + write(OFS_ProdOrdNum.map(_.prodOrderNum.toLong)))
        logger.info("Completed APJ-OFS backlog processing")
      }
      case Failure(e) =>
        logger.error("Event_id- "+incomingMessage.event_ID.toString+" || Failed Delete Header and Detail API", e.printStackTrace)
        kafkaProducer.pushMessagetoRetry(incomingMessage, "Failed During Delete Header and Detail")
    }(execCon)
  }*/

  def backlogProcessOPR(url: String, incomingMessage: CommonEntity, salesOrderRef: String): Unit = {
    val dbhf = Future[Unit] {
      cmnapiUtil.deleteBacklogHeader(incomingMessage.sales_ORDER_ID, incomingMessage.buid)
    }(execCon)
    val dbdf = Future[Unit] {
      cmnapiUtil.deleteBacklogDetail(incomingMessage.sales_ORDER_ID, incomingMessage.buid)
    }(execCon)

    Future.sequence(List(dbhf, dbdf)).onComplete {
      case Success(x) => {
        logger.info("Delete Header and Detail Completed for event id " + incomingMessage.event_ID)
        cmnapiUtil.updateBacklog(url, incomingMessage.buid.toString, incomingMessage.event_ID.toString, incomingMessage.sales_ORDER_ID, salesOrderRef)
        logger.info("Completed APJ-OPR backlog processing")
      }
      case Failure(e) =>
        logger.error("Event_id- "+incomingMessage.event_ID.toString+" || Failed Delete Header and Detail API", e.printStackTrace)
        kafkaProducer.pushMessagetoRetry(incomingMessage, "Failed During Delete Header and Detail")
    }(execCon)
  }

/*  def backlogProcessDrgn(url: String, incomingMessage: CommonEntity): Unit = {
    val dbhf = Future[Unit] {
      cmnapiUtil.deleteDRGNBacklogDetail(incomingMessage.sales_ORDER_ID, incomingMessage.buid, "DRAGON", incomingMessage.event_ID)
    }(execCon)
    val dbdf = Future[Unit] {
      cmnapiUtil.deleteDRGNBacklogHeader(incomingMessage.sales_ORDER_ID, incomingMessage.buid, "DRAGON")
    }(execCon)

    Future.sequence(List(dbhf, dbdf)).onComplete {
      case Success(x) => {
        logger.info("Delete Header and Detail Completed")
        cmnapiUtil.drgnBacklog(url, incomingMessage.buid.toString, "DRAGON", incomingMessage.event_ID.toString, incomingMessage.sales_ORDER_ID)
        logger.info("Completed Dragon backlog processing")
      }
      case Failure(e) =>
        logger.error("Event_id- "+incomingMessage.event_ID.toString+" || Failed Delete Header and Detail API", e.printStackTrace)
        kafkaProducer.pushMessagetoRetry(incomingMessage, "Failed During Delete Header and Detail")
    }(execCon)
  }*/

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