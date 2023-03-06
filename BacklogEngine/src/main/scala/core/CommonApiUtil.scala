package core

import code.RuleEngine.{cmnent, inme, logger}
import common.AkkaHttp
import constant.PropertiesLoader
import entities.{BacklogHeaderDetailsResp, ObjLSKU, ObjPart, ObjProdOrder, ObjProdOrderUnit, ObjSalesOrder, ObjSalesOrderLine, ObjSoAttRef, Obj_DRGN_FULFILLMENT, Obj_DRGN_ORDER_DETAIL, Obj_DRGN_ORDER_HEADER}
import kafka.KafkaProducer
import org.json4s.{DefaultFormats, JNull}
import org.json4s.jackson.JsonMethods.parse

import scala.collection.mutable.ArrayBuffer

class CommonApiUtil {
  val cmndbUtil = new CommonDBUtil(cmnent, inme)
  val kfkProdcuer = new KafkaProducer()

  implicit val formats = DefaultFormats

  def getSalesOrder(url: String, SalesOrderId: String, buid: String, eventId: Long): ObjSalesOrder = {
    try {
      val msEndPoint = url + "salesOrderId=" + SalesOrderId + "&buid=" + buid
      logger.info("Event_Id- " + eventId + "|| .... Get SalesOrder .... \nAPI URL- " + msEndPoint)
      val responseData = AkkaHttp.getResponse(msEndPoint, eventId)
      if (responseData != null) {
        val resp = (parse(responseData) \\ "data")
        if (resp != JNull)
          return resp.extract[ObjSalesOrder]
      }
      null
    }
    catch {
      case e =>
        logger.error("Get Sales Order failed for event id " + eventId + e.getCause)
        null
    }
  }

  def getProdOrder(url: String, SalesOrderRefId: String, buid: String, region: String, eventId: Long): ArrayBuffer[ObjProdOrder] = {
    try {
      val msEndPoint = url + "salesOrderRef=" + SalesOrderRefId + "&buid=" + buid + "&region=" + region
      logger.info("Event_Id- " + eventId + "|| .... Get ProdOrder .... \nAPI URL- " + msEndPoint)
      val responseData = AkkaHttp.getResponse(msEndPoint, eventId)

      if (responseData != null) {
        val resp = (parse(responseData) \\ "data")
        if (resp != JNull)
          return resp.extract[ArrayBuffer[ObjProdOrder]]
      }
      null
    } catch {
      case e =>
        logger.error("Get Prod Order failed for event id " + eventId + e.getCause)
        null
    }
  }

  def getSalesOrderLine(url: String, isMfgCheckRequired: Boolean, salesOrderRef: String, status: String, eventId: Long): ArrayBuffer[ObjSalesOrderLine] = {
    try {
      val msEndPoint = url + "isMfgCheckRequired=" + isMfgCheckRequired + "&salesOrderRef=" + salesOrderRef
      logger.info("Event_Id- " + eventId + "|| .... Get SalesOrderLine .... \nAPI URL- " + msEndPoint)
      val responseData = AkkaHttp.getResponse(msEndPoint, eventId)

      if (responseData != null) {
        val resp = (parse(responseData) \\ "data")
        if (resp != JNull)
          return resp.extract[ArrayBuffer[ObjSalesOrderLine]]
      }
      null
    } catch {
      case e =>
        logger.error("Get Sales Order Line failed for event id " + eventId + e.getCause)
        null
    }
  }

  def getProdUnit(msEndPoint: String, soLineRef: String, eventId: Long): ArrayBuffer[ObjProdOrderUnit] = {
    try {
      logger.info("Event_Id- " + eventId + "|| .... Get ProdOrdUnit .... \nAPI URL- " + msEndPoint +
        "\nAPI input parameter passed- " + soLineRef)
      val responseData = AkkaHttp.postRequest(msEndPoint, soLineRef, eventId)

      if (responseData != null) {
        val resp = (parse(responseData) \\ "data")
        if (resp != JNull)
          return resp.extract[ArrayBuffer[ObjProdOrderUnit]]
      }
      null
    } catch {
      case e =>
        logger.error("Get Prod Order Unit failed for event id " + eventId + e.getCause)
        null
    }
  }

  def getSOAttrRef(url: String, SalesOrderRefId: String, region: String, eventId: Long): ArrayBuffer[ObjSoAttRef] = {
    try {
      val msEndPoint = url + "region=" + region + "&salesOrderRef=" + SalesOrderRefId
      logger.info("Event_Id- " + eventId + "|| .... Get ProdOrder .... \nAPI URL- " + msEndPoint)
      val responseData = AkkaHttp.getResponse(msEndPoint, eventId)

      if (responseData != null) {
        val resp = (parse(responseData) \\ "data")
        if (resp != JNull)
          return resp.extract[ArrayBuffer[ObjSoAttRef]]
      }
      null
    } catch {
      case e =>
        logger.error("Get Prod Order failed for event id " + eventId + e.getCause)
        null
    }
  }


  def deleteBacklogDetail(orderNumber: String, buid: Int): Unit = {
    try {
      val msEndPoint = PropertiesLoader.Delete_Backlog_Detail + "buid=" + buid + "&orderNumber=" + orderNumber
      logger.info(" .... Deleting existing backlog detail records .... \nAPI URL- " + msEndPoint)
      AkkaHttp.deleteRequest(msEndPoint)
    } catch {
      case e =>
        logger.error("Delete Backlog Detail failed for orderNumber" + orderNumber + e.getCause)
        throw e
    }
  }

  def deleteBacklogHeader(orderNumber: String, buid: Int): Unit = {
    try {
      val msEndPoint = PropertiesLoader.Delete_Backlog_Header + "buid=" + buid + "&orderNumber=" + orderNumber
      logger.info(" .... Deleting existing backlog header records .... \nAPI URL- " + msEndPoint)
      AkkaHttp.deleteRequest(msEndPoint)
    } catch {
      case e =>
        logger.error("Delete Backlog Header failed for orderNumber" + orderNumber, e)
        throw e
    }
  }

  def deleteDRGNBacklogDetail(orderNumber: String, buid: Int, source: String, eventId: Long): Unit = {
    try {
      val msEndPoint = PropertiesLoader.Delete_DRGN_Backlog_Detail + "buid=" + buid + "&orderNumber=" + orderNumber + "&sourcesys=" + source + "&eventId=" + eventId
      logger.info("Event_Id- " + eventId + "|| .... Deleting existing Dragon backlog detail records .... \nAPI URL- " + msEndPoint)
      AkkaHttp.deleteRequest(msEndPoint)
    } catch {
      case e =>
        logger.error("Get Dragon Detail failed for event id " + eventId + e.getCause)
        throw e
    }
  }

  def deleteDRGNBacklogHeader(orderNumber: String, buid: Int, source: String): Unit = {
    try {
      val msEndPoint = PropertiesLoader.Delete_DRGN_Backlog_Header + "buid=" + buid + "&orderNumber=" + orderNumber + "&sourcesys=" + source
      logger.info(" .... Deleting existing Dragon backlog header records .... \nAPI URL- " + msEndPoint)
      AkkaHttp.deleteRequest(msEndPoint)
    }
    catch {
      case e =>
        logger.error("Get Dragon Header failed Order Number " + orderNumber + e.getCause)
        throw e
    }
  }

  def getSOAttribute(salesOrderRef: String, attributeName: String, eventId: Long): String = {
    try {
      val msEndPoint = PropertiesLoader.SOAttributeUrl + "salesOrderRef=" + salesOrderRef + "&attributeName=" + attributeName
      logger.info("Event_Id- " + eventId + "|| .... Get SOAttribute .... \nAPI URL- " + msEndPoint)
      val responseData = AkkaHttp.getResponse(msEndPoint, eventId)
      if (responseData == null) {
        "N"
      }
      else {
        (parse(responseData) \\ "data" \\ "attributeValue").extract[String]
      }
    } catch {
      case e =>
        logger.error("Get SOA Attribute failed for event id " + eventId + e.getCause)
        throw e
    }
  }

  def getLineSkuPart(url: String, line_sku_ref: String, status: String, eventId: Int): ArrayBuffer[ObjPart] = {
    try {
      val msEndPoint = url + "lineSkuRef=" + line_sku_ref
      logger.info("Event_Id- " + eventId + "|| .... Get LineSkuPart .... \nAPI URL- " + msEndPoint)
      val responseData = AkkaHttp.getResponse(msEndPoint, eventId)

      if (responseData != null) {
        val resp = (parse(responseData) \\ "data")
        if (resp != JNull)
          return resp.extract[ArrayBuffer[ObjPart]]
      }
      null
    } catch {
      case e =>
        logger.error("Get lineSkuPart failed for event id " + eventId + e.getCause)
        null
    }
  }

  def getlskuData(msEndPoint: String, salesorderlineref: String, eventId: Long): ArrayBuffer[ObjLSKU] = {
    try {
      logger.info("Event_Id- " + eventId + "|| .... Get LineSku .... \nAPI URL- " + msEndPoint +
        "\nAPI input parameter passed- " + salesorderlineref)
      val responseData = AkkaHttp.postRequest(msEndPoint, salesorderlineref, eventId)

      if (responseData != null) {
        val resp = (parse(responseData) \\ "data")
        if (resp != JNull)
          return resp.extract[ArrayBuffer[ObjLSKU]]
      }
      null
    } catch {
      case e =>
        logger.error("Get LineSKU failed for event id " + eventId + e.getCause)
        null
    }
  }

  def getPartData(msEndPoint: String, lineskuref: String, eventId: Long): ArrayBuffer[ObjPart] = {
    try {
      logger.info("Event_Id- " + eventId + "|| .... Get PartData .... \nAPI URL- " + msEndPoint +
        "\nAPI input parameter passed- " + lineskuref)
      if (lineskuref.length > 20) {
        val responseData = AkkaHttp.postRequest(msEndPoint, lineskuref, eventId)

        if (responseData != null) {
          val resp = (parse(responseData) \\ "data")
          if (resp != JNull)
            return resp.extract[ArrayBuffer[ObjPart]]
        }
      }
      null
    } catch {
      case e =>
        logger.error("Get Part failed for event id " + eventId + e.getCause)
        null
    }
  }

  def updateBacklog(url: String, buid: String, eventid: String, salesorderId: String, salesOrderRef: String): Unit = {
    try {
      val json_data = "{\"buid\":" + buid + ",\"eventid\":" + eventid + ",\"salesorderId\":" + salesorderId + ",\"salesOrderRef\":" + salesOrderRef + "}"
      logger.info(" Event_id- " + eventid + " || .... Started Backlog Processing for APJ-OPR .... \nAPI URL- " + url +
        "\nAPI input parameter passed- " + json_data)
      val responseData = AkkaHttp.postRequest(url, json_data, eventid.toLong)
      //logger.info(" .... Backlog Processing for APJ-OPR response .... " + responseData, SystemEnum.API)
      logger.info("Event_Id- " + eventid + "||  .... Backlog Processing for APJ-OPR response .... " + responseData)
      if (responseData != null)
        fetchStatus(parse(responseData).extract[BacklogHeaderDetailsResp], eventid, false)
      else {
        logger.info("Event_Id- " + eventid + "|| postRequest response is null..push data to queue")
        val message = cmndbUtil.getMessage(eventid.toLong)
        kfkProdcuer.pushMessagetoRetry(message, "Post Request Failed ")
      }
    } catch {
      case e =>
        logger.error("Backlog APJ OPR failed for eventid " + eventid + e)
        throw e
    }
  }

  def updateEmeaBacklog(url: String, buid: String, eventid: String, salesorderId: String) {
    try {
      val json_data = "{\"buid\":" + buid + ",\"eventId\":" + eventid + ",\"orderNumber\":\"" + salesorderId + "\"}"
      logger.info("Event_Id- " + eventid + "||  .... Started Backlog Processing for EMEA .... \nAPI URL- " + url +
        "\nAPI input parameter passed- " + json_data)
      val responseData = AkkaHttp.postRequest(url, json_data, eventid.toLong)
      logger.info("Event_Id- " + eventid + "|| .... Backlog Processing for EMEA response .... " + responseData)
      if (responseData != null)
        fetchStatus(parse(responseData).extract[BacklogHeaderDetailsResp], eventid, false)
      else {
        logger.info("Event_Id- " + eventid + "|| postRequest response is null..push data to queue")
        val message = cmndbUtil.getMessage(eventid.toLong)
        kfkProdcuer.pushMessagetoRetry(message, "Post Request Failed ")
      }
    }
    catch {
      case e =>
        logger.error("Backlog EMEA failed eventid " + eventid + e)
        throw e
    }
  }

  def updateBacklogOFS(url: String, buid: String, eventid: String, salesorderId: String, prodOrderNum: String): Unit = {
    try {
      val json_data = "{\"buid\":\"" + buid + "\",\"eventId\":\"" + eventid + "\",\"orderNum\":\"" + salesorderId + "\"," + prodOrderNum + "}"
      logger.info("Event_Id- " + eventid + "||  .... Started Backlog Processing for APJ-OFS .... \nAPI URL- " + url +
        "\nAPI input parameter passed- " + json_data)
      val responseData = AkkaHttp.postRequest(url, json_data, eventid.toLong)
      logger.info("Event_Id- " + eventid + "||  .... Backlog Processing for APJ-OFS response .... " + responseData)
      if (responseData != null)
        fetchStatus(parse(responseData).extract[BacklogHeaderDetailsResp], eventid, false)
      else {
        logger.info("Event_Id- " + eventid + "|| postRequest response is null..push data to queue")
        val message = cmndbUtil.getMessage(eventid.toLong)
        kfkProdcuer.pushMessagetoRetry(message, "Post Request Failed ")
      }
    }
    catch {
      case e =>
        logger.error("Backlog OFS failed eventid " + eventid + e)
        throw e
    }
  }

  def updateBacklogDao(url: String, buid: String, eventid: String, salesorderId: String): Unit = {
    try {
      val json_data = "{\"buid\":" + buid + ",\"eventId\":" + eventid + ",\"orderNumber\":\"" + salesorderId + "\"}"
      logger.info("Event_Id- " + eventid + "||  .... Started Backlog Processing for DAO .... \nAPI URL- " + url +
        "\nAPI input parameter passed- " + json_data)
      val responseData = AkkaHttp.postRequest(url, json_data, eventid.toLong)
      logger.info("Event_Id- " + eventid + "|| .... Backlog Processing for DAO response .... " + responseData)
      if (responseData != null)
        fetchStatus(parse(responseData).extract[BacklogHeaderDetailsResp], eventid, false)
      else {
        logger.info("Event_Id- " + eventid + "|| postRequest response is null..push data to queue")
        val message = cmndbUtil.getMessage(eventid.toLong)
        kfkProdcuer.pushMessagetoRetry(message, "Post Request Failed ")
      }
    } catch {
      case e =>
        logger.error("Backlog DAO failed eventid " + eventid + e)
        throw e
    }
  }

  def drgnBacklog(url: String, buid: String, ChannelCode: String, EventId: String, OrderNum: String): Unit = {
    try {
      val json_data = "{\"buid\":\"" + buid + "\",\"channelCode\":\"" + ChannelCode + "\",\"eventId\":\"" + EventId + "\",\"orderNumber\":\"" + OrderNum + "\"}"
      logger.info("Event_Id- " + EventId + "||  .... Started Backlog Processing for Dragon backlog .... \nAPI URL- " + url +
        "\nAPI input parameter passed- " + json_data)
      val responseData = AkkaHttp.postRequest(url, json_data, EventId.toLong)
      logger.info("Event_Id- " + EventId + "|| .... Backlog Processing for Dragon response .... " + responseData)
      if (responseData != null)
        fetchStatus(parse(responseData).extract[BacklogHeaderDetailsResp], EventId, false)
      else {
        logger.info("Event_Id- " + EventId + "|| postRequest response is null..push data to queue")
        val message = cmndbUtil.getMessage(EventId.toLong)
        kfkProdcuer.pushMessagetoRetry(message, "Post Request Failed ")
      }
    }
    catch {
      case e =>
        logger.error("Dragon Backlog failed eventid " + EventId + e)
        throw e
    }
  }

  //Dragon Specific
  def getDragOrderDetail(url: String, buid: String, sonum: String, isDO: Boolean, eventId: Long): ArrayBuffer[Obj_DRGN_ORDER_DETAIL] = {
    try {
      val msEndPoint = url + "?buid=" + buid + "&isDo=" + isDO + "&orderNum=" + sonum
      logger.info("Event_Id- " + eventId + "|| .... Get DragonOrderDetail .... \nAPI URL- " + msEndPoint)
      val json_data = AkkaHttp.getResponse(msEndPoint, eventId)

      if (json_data != null) {
        val resp = (parse(json_data) \\ "data")
        if (resp != JNull)
          return resp.extract[ArrayBuffer[Obj_DRGN_ORDER_DETAIL]]
      }
      new ArrayBuffer[Obj_DRGN_ORDER_DETAIL]
    }
    catch {
      case e =>
        logger.error("Get Dragon Order Details failed eventid " + eventId + e)
        throw e
    }
  }

  def getDragOrderHeader(url: String, buid: String, OrderNum: String, eventId: Long): ArrayBuffer[Obj_DRGN_ORDER_HEADER] = {
    try {
      val msEndPoint = url + "?buid=" + buid + "&orderNum=" + OrderNum
      logger.info("Event_Id- " + eventId + "|| .... Get DragonOrderHeader .... \nAPI URL- " + msEndPoint)
      val json_data = AkkaHttp.getResponse(msEndPoint, eventId)
      if (json_data != null) {
        val resp = (parse(json_data) \\ "data")
        if (resp != JNull)
          return resp.extract[ArrayBuffer[Obj_DRGN_ORDER_HEADER]]
      }
      new ArrayBuffer[Obj_DRGN_ORDER_HEADER]
    }
    catch {
      case e =>
        logger.error("Get Dragon Header failed eventid " + eventId + e)
        throw e
    }
  }

  def getDragOrderFul(url: String, buid: String, OrderNum: String, eventId: Long): ArrayBuffer[Obj_DRGN_FULFILLMENT] = {
    try {
      val msEndPoint = url + "?buid=" + buid + "&orderId=" + OrderNum
      logger.info("Event_Id- " + eventId + "|| .... Get DragonOrderFulfillment .... \nAPI URL- " + msEndPoint)
      val json_data = AkkaHttp.getResponse(msEndPoint, eventId)
      if (json_data != null) {
        val resp = (parse(json_data) \\ "data")
        if (resp != JNull)
          return resp.extract[ArrayBuffer[Obj_DRGN_FULFILLMENT]]
      }
      new ArrayBuffer[Obj_DRGN_FULFILLMENT]
    } catch {
      case e =>
        logger.error("Get Dragon Order Fulfillment failed eventid " + eventId + e)
        throw e
    }
  }

  def fetchStatus(responseData: BacklogHeaderDetailsResp, eventID: String, coreInsertChk: Boolean): Unit = {
    try {
      val msg = responseData.message
      if (!responseData.errorCode.equals(0)) {
        val message = cmndbUtil.getMessage(eventID.toLong)
        kfkProdcuer.pushMessagetoRetry(message, "response from backlog process " + msg)
        logger.info("Event_Id- " + eventID + "|| errCd " + responseData.errorCode)
      }
      else {
        if (coreInsertChk)
          cmndbUtil.updateStatus(eventID.toLong, "Completed", "NA", if (msg == null) "" else msg, 0);
        else
          cmndbUtil.updateStatus(eventID.toLong, "WIP", "Raw Backlog", if (msg == null) "" else msg, 0);
      }
    }
    catch {
      case e =>
        logger.error("Fetch Status failed eventid " + eventID + e)
        throw e
    }
  }

  def insertBacklog(buId: String, eventId: String, salesOrderId: String): Unit = {

    try {

      val json_data = "{\"buid\":" + buId + ",\"eventId\":" + eventId + ",\"orderNumber\":\"" + salesOrderId + "\"}"

      logger.info("Event_Id- " + eventId + "|| .... Insert into Core Backlog .... \nAPI URL- " + PropertiesLoader.Backlog_insert +

        "\nAPI input parameter passed- " + json_data)
    val responseData = AkkaHttp.postRequest(PropertiesLoader.Backlog_insert, json_data, eventId.toLong)

    println("Response data : core API insert " + responseData)

    if (responseData != null)

      fetchStatus(parse(responseData).extract[BacklogHeaderDetailsResp], eventId, true)

    else {

      logger.info("postRequest response is null..push data to queue")

      val message = cmndbUtil.getMessage(eventId.toLong)

      kfkProdcuer.pushMessagetoRetry(message, "Post Request Failed ")

    }

    null
    }


  catch
  {

    case e =>

      logger.error("Insertion to backlog tables failed for event id " + eventId + e.getCause)

      null

  }

}

def thirdPartyLogic (buId: String, eventId: String, salesOrderId: String): Unit = {

  try {

  val json_data = "{\"buid\":" + buId + ",\"eventId\":" + eventId + ",\"orderNumber\":\"" + salesOrderId + "\"}"

  logger.info ("Event_Id- " + eventId + "|| .... 3PL consumption API .... \nAPI URL- " + PropertiesLoader.API_3PL +

  "\nAPI input parameter passed- " + json_data)


  val responseData = AkkaHttp.putRequest (PropertiesLoader.API_3PL, json_data, eventId.toLong)

  println ("Response data : 3PL API update " + responseData)

  if (responseData != null) {

  val resp = (parse (responseData) \\ "data")

  if (resp != JNull)

  return resp.extract[ArrayBuffer[BacklogHeaderDetailsResp]]

}

  null

} catch {

  case e =>

  logger.error ("3PL consumption API failed for event id " + eventId + e.getCause)

  null

}

}


  def insertBklgIm (SalesOrderId: Long, buid: Long, eventId: Long): Unit = {

  try {

  val msEndPoint = PropertiesLoader.IM_Backlog_Insert

  val json_data = "{\"buid\":" + buid + ",\"eventId\":" + eventId + ",\"orderNumber\":\"" + SalesOrderId + "\"}"

  logger.info ("Event_Id- " + eventId + "|| .... Preparing RAW backlog .... \nAPI URL- " + msEndPoint)

  val responseData = AkkaHttp.postRequest (msEndPoint, json_data, eventId)

  if (responseData != null)

  fetchStatus (parse (responseData).extract[BacklogHeaderDetailsResp], eventId.toString, false)

  else {

  logger.info ("postRequest response is null..push data to queue")

  val message = cmndbUtil.getMessage (eventId.toLong)

  kfkProdcuer.pushMessagetoRetry (message, "Post Request Failed ")

}

  null

} catch {

  case e =>

  logger.error ("Preparation of raw backlog from core backlog failed for event id " + eventId + e.getCause)

  null

}

}
}
