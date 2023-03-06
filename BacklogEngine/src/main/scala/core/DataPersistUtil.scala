package core

import java.text.SimpleDateFormat
//import java.time.format.DateTimeFormatter
import java.util.Calendar
import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, AllForOneStrategy, PoisonPill, Props, Terminated}
import code.Orchestrator.{backlogEngineActorsystem, execCon}
import code.RuleEngine.{cmnent, logger}
import common.DataPersistHandler
import constant.UPPBEConstants
import entities.{CommonEntity, DRGN_FULFILLMENT, DRGN_ORDER_DETAIL, DRGN_ORDER_HEADER, LSKU, ObjLSKU, ObjPart, ObjProdOrder, ObjProdOrderUnit, ObjSalesOrder, ObjSalesOrderLine, ObjSoAttRef, Obj_DRGN_FULFILLMENT, Obj_DRGN_ORDER_DETAIL, Obj_DRGN_ORDER_HEADER, Part, ProdOrder, ProdOrderUnit, SalesOrder, SalesOrderLine, SoAttRef}
import kafka.KafkaProducer
import net.liftweb.json.DefaultFormats

import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.collection.JavaConverters._
import scala.concurrent.Future

object DataPersistUtil {
  var localProcess = false
  //val PersistActor = backlogEngineActorsystem.actorOf(Props[DataPersist], "UPPBE_DataPersist_ACTOR")

  val dataPersistUtil = new DataPersistHandler(cmnent)

  object DataPersist {
    implicit val formats = DefaultFormats

    case class persistSalesOrder(incomingMessage: CommonEntity, salesOrder: ObjSalesOrder)

    case class persistProdOrder(incomingMessage: CommonEntity, tableName: String, prodOrder: ArrayBuffer[ObjProdOrder])

    case class persistSOL(incomingMessage: CommonEntity, tableName: String, sol: ArrayBuffer[ObjSalesOrderLine])

    case class persistProdOrdUnit(incomingMessage: CommonEntity, tableName: String, prodUnit: ArrayBuffer[ObjProdOrderUnit])

    case class persistlsku(incomingMessage: CommonEntity, tableName: String, lsku: ArrayBuffer[ObjLSKU])

    case class persistPart(incomingMessage: CommonEntity, tableName: String, part: ArrayBuffer[ObjPart])

    case class persistDragOrdHeader(dragOrdHeader: ArrayBuffer[Obj_DRGN_ORDER_HEADER], incomingMessage: CommonEntity)

    case class persistDragOrderDetail(dragOrdDetail: ArrayBuffer[Obj_DRGN_ORDER_DETAIL], incomingMessage: CommonEntity)

    case class persistDragOrderFulfillment(dragOrdFulfillment: ArrayBuffer[Obj_DRGN_FULFILLMENT], incomingMessage: CommonEntity)

  }

  class DataPersist extends Actor with ActorLogging {
    val kafProducer = new KafkaProducer()

    import DataPersist._
    //val logger: LoggingUtil = new LoggingUtil(ExperienceEnum.MTRC, "UPP_Backlog_Engine", "UPP_Backlog_Engine_KafkaReadWrite", "UPPBE", "UPPBE_ControlM_JobName", "1.0-SNAPSHOT", "9999999999", "Dev")

    override def preStart = {
      //logger.info((" .... Starting Data Persist .... " , SystemEnum.API)
      log.info(" .... Starting Data Persist .... ")
    }

    override val supervisorStrategy = AllForOneStrategy() {
      case _: Exception => {
        //logger.info(("Error while publishing the message to SRE : ", SystemEnum.API)
        log.info("Exception in Master Orchestrator")
        Stop
      }
    }

    override def receive: Receive = {
      case message: String => logger.info(message)

      case persistSalesOrder(msg, salesOrder) => {
        if (salesOrder != null) {
          try {
            dataPersistUtil.writeDatatoTable(UPPBEConstants.SALES_ORDER_ENTITY, convertObjSOToSO(msg.event_ID, salesOrder))
            sender() ! msg.event_ID.toString()+" Persisted"
            logger.info("Event_id- "+msg.event_ID.toString()+"!!!IM_SALES_ORDER data persisted!!!")
          }
          catch {
            case ex => {

              logger.error("SQLException while inserting records in IM_SALES_ORDER : " + ex.printStackTrace)
              //logger.error((" SQLException while inserting records in IM_SALES_ORDER : " + ExceptionUtils.getStackTrace(ex), ex, "")
              ex.getMessage
              kafProducer.pushMessagetoRetry(msg, "SQLException while inserting records in IM_SALES_ORDER")
            }
          }
        }
      }

      case persistProdOrder(msg, tableName: String, prodOrder: ArrayBuffer[ObjProdOrder]) => {
        if (!prodOrder.isEmpty) {
          try {
            dataPersistUtil.writeDatatoTable(tableName, convertObjPoToPo(msg.event_ID, prodOrder))
            sender() ! msg.event_ID.toString()+" Persisted"
            logger.info("!!!IM_PROD_ORDER data persisted!!!")
            //logger.info(("!!!IM_PROD_ORDER data persisted!!!" , SystemEnum.API)
          }
          catch {
            case ex => {

              logger.error("SQLException while inserting records in IM_PROD_ORDER: " + ex.printStackTrace)
              //logger.error((" SQLException while inserting records in IM_PROD_ORDER: " + ExceptionUtils.getStackTrace(ex), ex, "")
              ex.getMessage
              kafProducer.pushMessagetoRetry(msg, "SQLException while inserting records in IM_PROD_ORDER")

            }
          }
        }
      }

      case persistSOL(msg, tableName: String, solOrder: ArrayBuffer[ObjSalesOrderLine]) => {
        if (!solOrder.isEmpty) {
          try {
            dataPersistUtil.writeDatatoTable(tableName, convertObjSOLToSOL(msg.event_ID, solOrder))
            sender() ! msg.event_ID.toString()+" Persisted"
            logger.info("!!!IM_SALES_ORDER_LINE data persisted!!!")
            //logger.info(("!!!IM_SALES_ORDER_LINE data persisted!!!" , SystemEnum.API)
          }
          catch {
            case ex => {

              logger.error("SQLException while inserting records in IM_SALES_ORDER_LINE: " + ex.printStackTrace)
              //logger.error((" SQLException while inserting records in IM_SALES_ORDER_LINE: " + ExceptionUtils.getStackTrace(ex), ex, "")
              ex.getMessage
              kafProducer.pushMessagetoRetry(msg, "SQLException while inserting records in IM_SALES_ORDER_LINE")
            }
          }
        }
      }

      case persistProdOrdUnit(msg, tableName: String, prodUnit: ArrayBuffer[ObjProdOrderUnit]) => {
        if (!prodUnit.isEmpty) {
          try {
            dataPersistUtil.writeDatatoTable(tableName, convertObjPOUToPOU(msg.event_ID, prodUnit))
            sender() ! msg.event_ID.toString()+" Persisted"
            logger.info("!!!IM_PROD_ORDER_UNIT data persisted!!!")
            //logger.info(("!!!IM_PROD_ORDER_UNIT data persisted!!!" , SystemEnum.API)
          }
          catch {
            case ex => {

              logger.error("SQLException while inserting records in IM_PROD_ORDER_UNIT: " + ex.printStackTrace)
              //logger.error((" SQLException while inserting records in IM_PROD_ORDER_UNIT: " + ExceptionUtils.getStackTrace(ex), ex, "")

              ex.getMessage
              kafProducer.pushMessagetoRetry(msg, "SQLException while inserting records in IM_PROD_ORDER_UNIT")
            }
          }
        }
      }

      case persistlsku(msg, tableName: String, solsku: ArrayBuffer[ObjLSKU]) => {
        if (!solsku.isEmpty) {
          try {
            dataPersistUtil.writeDatatoTable(tableName, convertObjLSKUToLSKU(msg.event_ID, solsku))
            logger.info("!!!IM_LINE_SKU data persisted!!!")
            sender() ! msg.event_ID.toString()+" Persisted"
          }
          catch {
            case ex => {

              logger.error("SQLException while inserting records in IM_LINE_SKU: " + ex.printStackTrace)
              //logger.error((" SQLException while inserting records in IM_LINE_SKU: " + ExceptionUtils.getStackTrace(ex), ex, "")
              ex.getMessage
              kafProducer.pushMessagetoRetry(msg, "SQLException while inserting records in IM_LINE_SKU")
            }
          }
        }
      }
      case persistPart(msg, tableName: String, lskuParts: ArrayBuffer[ObjPart]) => {
        if (!lskuParts.isEmpty) {
          try {
            dataPersistUtil.writeDatatoTable(tableName, convertObjPartToPart(msg.event_ID, lskuParts))
            logger.info("!!!IM_PART data persisted!!!")
            sender() ! msg.event_ID.toString()+" Persisted"
          }
          catch {
            case ex => {

              logger.error("SQLException while inserting records in IM_PART: " + ex.printStackTrace)
              //logger.error((" SQLException while inserting records in IM_PART: " + ExceptionUtils.getStackTrace(ex), ex, "")
              ex.getMessage
              kafProducer.pushMessagetoRetry(msg, "SQLException while inserting records in IM_PART")
            }
          }
        }
      }

      case persistDragOrderDetail(dragOrdDetail: ArrayBuffer[Obj_DRGN_ORDER_DETAIL], msg) => {
          try {
            if (!dragOrdDetail.isEmpty)
              dataPersistUtil.writeDatatoTable(UPPBEConstants.DRGN_ORDER_DETAIL_ENTITY, convertObjDDToDD(msg.event_ID, dragOrdDetail))
            sender() ! msg.event_ID.toString()+" Persisted"
            logger.info("!!!IM_DRGN_ORDER_DETAIL data persisted!!!")
          }
          catch {
            case ex => {

              logger.error("SQLException while inserting records in IM_DRGN_ORDER_DETAIL: " + ex.printStackTrace)
              //logger.error((" SQLException while inserting records in IM_DRGN_ORDER_DETAIL:" + ExceptionUtils.getStackTrace(ex), ex, "")
              ex.getMessage
              kafProducer.pushMessagetoRetry(msg, "SQLException while inserting records in IM_DRGN_ORDER_DETAIL")
            }
          }
      }

      case persistDragOrdHeader(dragOrdHeader: ArrayBuffer[Obj_DRGN_ORDER_HEADER], msg) => {
          try {
            if (!dragOrdHeader.isEmpty)
              dataPersistUtil.writeDatatoTable(UPPBEConstants.DRGN_ORDER_HEADER_ENTITY, convertObjDHToDH(msg.event_ID, dragOrdHeader))
            sender() ! msg.event_ID.toString()+" Persisted"
            logger.info("!!!IM_DRGN_ORDER_HEADER data persisted!!!")
          }
          catch {
            case ex => {
              logger.error("SQLException while inserting records in IM_DRGN_ORDER_HEADER: " + ex.printStackTrace)
              //logger.error((" SQLException while inserting records in IM_DRGN_ORDER_HEADER: " + ExceptionUtils.getStackTrace(ex), ex, "")
              ex.getMessage
              kafProducer.pushMessagetoRetry(msg, "SQLException while inserting records in IM_DRGN_ORDER_HEADER")
            }
          }
      }

      case persistDragOrderFulfillment(dragOrdFulfillment: ArrayBuffer[Obj_DRGN_FULFILLMENT], msg) => {
        try {
          if (dragOrdFulfillment.size > 0)
            dataPersistUtil.writeDatatoTable(UPPBEConstants.DRGN_ORDER_FULFILLMENT_ENTITY, convertObjDFToDF(msg.event_ID, dragOrdFulfillment))
          sender() ! msg.event_ID.toString() + " Persisted"
          logger.info("!!!IM_DRGN_ORDER_FULFILLMENT data persisted!!!")
        }
        catch {
          case ex => {
            logger.error("SQLException while inserting records in IM_DRGN_ORDER_FULFILLMENT: " + ex.printStackTrace)
            ex.getMessage
            kafProducer.pushMessagetoRetry(msg, "SQLException while inserting records in IM_DRGN_ORDER_FULFILLMENT")
          }
        }
      }
      case Terminated(ref) => {
        log.info(s"$ref closed")
        context.self ! PoisonPill
        log.info("Checking if stopped")
        context.system.terminate()
      }
    }

    def endActor: Receive = {
      case persistSalesOrder(msg, salesOrder) => log.warning("Stopping as nothing to be persisted for SO")
      case persistProdOrder(msg, tableName, prodOrder) => log.warning("Stopping as nothing to be persisted for PO")
      case persistSOL(msg, tableName, sol) => log.warning("Stopping as nothing to be persisted for SOL")
      case persistlsku(msg, tableName, lsku) => log.warning("Stopping as nothing to be persisted for LSKU")
      case persistPart(msg, tableName, part) => log.warning("Stopping as nothing to be persisted for part")
    }

    override def postStop = {
      logger.info("End Time DataPersist: " + Calendar.getInstance.getTime)
    }
  }


  def convertObjSOToSO(eventId: Long, objSO: ObjSalesOrder): SalesOrder = {
    new SalesOrder(objSO.salesOrderId, objSO.salesOrderRef, objSO.region, objSO.sourceSystemStatusCode, objSO.buId,
      objSO.orderDate, objSO.ipDate, objSO.shipByDate, objSO.customerNum, objSO.companyNum, objSO.itemQty,
      objSO.systemQty, objSO.state, objSO.country, objSO.statusCode, objSO.shipCode, objSO.mustArriveByDate,
      objSO.fulfillmentStatusCode, eventId)
  }

  def convertObjPoToPo(eventId: Long, objPO: ArrayBuffer[ObjProdOrder]): java.util.List[ProdOrder] = {
    var lst = new ListBuffer[ProdOrder]()
    val inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

    objPO.foreach(prd => {
      lst += new ProdOrder(prd.prodOrderNum, prd.salesOrderRef, prd.ccn, prd.channelCode, prd.hasFga, prd.buildType,
        prd.facility, prd.statusCode, prd.heldCode, prd.primaryFc, prd.isRetail, prd.retailerName,
        prd.shipToFacility, prd.orderPriority,
        if (prd.updateDate != null && !prd.updateDate.isEmpty) new java.sql.Timestamp(inputFormat.parse(prd.updateDate).getTime()) else null,
        prd.subChannel, eventId,prd.boxingFacility.getOrElse(""),prd.kittingFacility.getOrElse(""))
    })
    lst.asJava
  }

  def convertObjSOLToSOL(eventId: Long, objSOL: ArrayBuffer[ObjSalesOrderLine]): java.util.List[SalesOrderLine] = {
    var lst = new ListBuffer[SalesOrderLine]()

    objSOL.foreach(sol => {
      lst += new SalesOrderLine(sol.soLineRef, sol.salesOrderRef, sol.prodOrderNum, sol.lineNum, sol.lineQty,
        sol.tieNum,sol.lob, sol.baseType, sol.workCenter, sol.ssc, sol.region, sol.mfgMethod,
        sol.mfgLob,eventId)
    })
    lst.asJava
  }

  def convertObjSoAttRefToSoAttRef(eventId: Long, objSOL: ArrayBuffer[ObjSoAttRef]): java.util.List[SoAttRef] = {
    var lst = new ListBuffer[SoAttRef]()
    val inputFormat = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss")
    val inputFormatT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

    objSOL.foreach(soA => {
      lst += new SoAttRef(soA.soAttributeRef,soA.salesOrderRef,soA.attributeName,soA.attributeValue,
        soA.createBy,
        if (soA.createDate != null && !soA.createDate.isEmpty) new java.sql.Timestamp(inputFormat.parse(soA.createDate).getTime()) else null,
        soA.updateBy,
        if (soA.updateDate != null && !soA.updateDate.isEmpty) new java.sql.Timestamp(inputFormat.parse(soA.updateDate).getTime()) else null,
        if (soA.srcTransTimestamp != null && !soA.srcTransTimestamp.isEmpty) new java.sql.Timestamp(inputFormatT.parse(soA.srcTransTimestamp).getTime()) else null,
        if (soA.updateTimestamp != null && !soA.updateTimestamp.isEmpty) new java.sql.Timestamp(inputFormat.parse(soA.updateTimestamp).getTime()) else null,
        soA.sourceDb,eventId)
    })
    lst.asJava
  }

  def convertObjPOUToPOU(eventId: Long, objPOU: ArrayBuffer[ObjProdOrderUnit]): java.util.List[ProdOrderUnit] = {
    var lst = new ListBuffer[ProdOrderUnit]()
    val inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

    objPOU.foreach(pou => {
      lst += new ProdOrderUnit(pou.soLineRef, pou.unitSeq, pou.buid, pou.svcTag, pou.region,
        pou.statusCode,
        if (pou.statusDate != null && !pou.statusDate.isEmpty) new java.sql.Timestamp(inputFormat.parse(pou.statusDate).getTime()) else null,
        if (pou.createDate != null && !pou.createDate.isEmpty) new java.sql.Timestamp(inputFormat.parse(pou.createDate).getTime()) else null,
        pou.createBy,
        if (pou.updateDate != null && !pou.updateDate.isEmpty) new java.sql.Timestamp(inputFormat.parse(pou.updateDate).getTime()) else null,
        pou.updateBy, pou.srcTransTimeStamp, pou.updateTimeStamp,eventId)
    })
    lst.asJava
  }

  def convertObjLSKUToLSKU(eventId: Long, objLSKU: ArrayBuffer[ObjLSKU]): java.util.List[LSKU] = {
    var lst = new ListBuffer[LSKU]()

    objLSKU.foreach(lsku => {
      lst += new LSKU(lsku.lineSkuRef, lsku.soLineRef, lsku.skuHdrRef, lsku.skuQty, lsku.skuNum,
        lsku.isFgaSku,lsku.modQty.getOrElse(0), lsku.baseFlag.getOrElse(""),
        lsku.fgaProxyItem.getOrElse(""), lsku.mfgPartNum.getOrElse(""),
        lsku.classCode.getOrElse(""), lsku.subClass.getOrElse(""), lsku.buid,
        lsku.salesOrderId, lsku.region,eventId)
    })
    lst.asJava
  }

  def convertObjPartToPart(eventId: Long, objPart: ArrayBuffer[ObjPart]): java.util.List[Part] = {
    var lst = new ListBuffer[Part]()

    objPart.foreach(part => {
      lst += new Part(part.lineSkuRef.getOrElse(""), part.partSeq.getOrElse(0), part.pLineSkuRef.getOrElse(0),
        part.pPartSeq.getOrElse(0),
        part.partNum.getOrElse(""),
        part.issueCode.getOrElse(0),part.boxCode.getOrElse(""), part.consumptionFacility.getOrElse(""),
        part.qtyPerSku.getOrElse(0), part.partType.getOrElse(""),
        part.region.getOrElse(""), part.createDate.getOrElse(""), part.dmsFlag.getOrElse(""),
        part.itemCategory.getOrElse(""),part.qty.getOrElse(0),eventId,part.isSystem.getOrElse(""))
    })
    lst.asJava
  }

  def convertObjDDToDD(eventId: Long, objDD: ArrayBuffer[Obj_DRGN_ORDER_DETAIL]): java.util.List[DRGN_ORDER_DETAIL] = {
    var lst = new ListBuffer[DRGN_ORDER_DETAIL]()

    objDD.foreach(dd => {
      lst += new DRGN_ORDER_DETAIL(dd.buid,dd.orderNum,dd.commodityCode.getOrElse(""), dd.ssc.getOrElse(""),
        dd.isFgaSku.getOrElse(""), dd.orderQty.getOrElse(0), dd.skuQty.getOrElse(0),dd.itemBomQty.getOrElse(0),
        dd.productType, dd.partNum.getOrElse(""),dd.tieNumber,dd.itemLineNum,
        dd.region.getOrElse(""),eventId)
    })
    lst.asJava
  }

  def convertObjDHToDH(eventId: Long, objDH: ArrayBuffer[Obj_DRGN_ORDER_HEADER]): java.util.List[DRGN_ORDER_HEADER] = {
    var lst = new ListBuffer[DRGN_ORDER_HEADER]()
    val inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

    objDH.foreach(dh => {
      lst += new DRGN_ORDER_HEADER(dh.prodOrderNum,dh.orderNum,dh.buid, dh.currentStatus,
        dh.ccnCode, dh.fulfillmentChannel, dh.customerNum,
        if (dh.orderDate != null && !dh.orderDate.isEmpty) new java.sql.Timestamp(inputFormat.parse(dh.orderDate).getTime()) else null,
        dh.centralLocation, dh.countryCode,dh.mergeOrderFlag,dh.status.getOrElse(""),
        dh.region.getOrElse(""),eventId, dh.ibu.getOrElse(""))
    })
    lst.asJava
  }

  def convertObjDFToDF(eventId: Long, objDF: ArrayBuffer[Obj_DRGN_FULFILLMENT]): java.util.List[DRGN_FULFILLMENT] = {
    var lst = new ListBuffer[DRGN_FULFILLMENT]()

    objDF.foreach(df => {
      lst += new DRGN_FULFILLMENT(df.buid,df.orderNum,df.status.getOrElse(""), df.updateDate,
        df.partNum.getOrElse(""), df.tieNum, df.itemLineNum,df.region.getOrElse(""),
        df.modNum.getOrElse(""), df.orderQty.getOrElse(0),eventId)
    })
    lst.asJava
  }
}
