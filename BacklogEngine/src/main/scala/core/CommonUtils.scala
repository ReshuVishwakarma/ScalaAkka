package core

import Irepository.ICommonEntityDAO
import code.RuleEngine.{inme, logger}
import config.{IConnection, MyBatisConnection}
import entities.{BacklogHeaderDetailsResp, ObjProdOrder, ObjSalesOrderLine}
import kafka.KafkaProducer
import repository.CommonEntityDAO
import com.google.inject.Inject

import scala.collection.mutable.ArrayBuffer

class CommonUtils {

  @Inject var sessionFac:IConnection = MyBatisConnection
  @Inject var ws: ICommonEntityDAO = new CommonEntityDAO(sessionFac)

  val cmndbUtil = new CommonDBUtil(ws,inme)
  val cmnapiUtil = new CommonApiUtil()
  val kfkProdcuer = new KafkaProducer()
  //val logger: LoggingUtil = new LoggingUtil(ExperienceEnum.MTRC, "UPP_Backlog_Engine", "UPP_Backlog_Engine_KafkaReadWrite", "UPPBE", "UPPBE_ControlM_JobName", "1.0-SNAPSHOT", "9999999999", "Dev")

  def checkProdOrderHasCCNValues(prodOrder: ArrayBuffer[ObjProdOrder]): Boolean = {
    if (prodOrder.filter(c => c.ccn == null || c.ccn.isEmpty).size > 0)
      false
    else
      true
  }

  def getGolfCount(salesOrderRef : String, eventId : Long): Int ={
    if (cmnapiUtil.getSOAttribute(salesOrderRef, "IsOTMEnabled",eventId).equalsIgnoreCase("Y"))
      1
    else if(cmnapiUtil.getSOAttribute(salesOrderRef, "IsGOLFEnabled",eventId).equalsIgnoreCase("Y"))
      1
    else
      0
  }
  def isEMCOrder(prodOrder: ArrayBuffer[ObjProdOrder]): Boolean = {
    val paramList = cmndbUtil.getParameterValue("TRIDENT_BOLERO_ORDERS", "TRIDENT_ORDER_FACILITY", "FDL_PROCESS")
    if (prodOrder.filter(c => paramList.contains(c.facility)).size == prodOrder.size)
      true
    else
      false
  }

  def isEMCMfgMethod(sol: ArrayBuffer[ObjSalesOrderLine]): Boolean = {
    val paramList = cmndbUtil.getParameterValue("TRIDENT_BOLERO_ORDERS")
    if (sol.filter(c => paramList.contains(c.mfgMethod)).size > 0)
      true
    else
      false
  }

  def hasFGA(prodOrder: ArrayBuffer[ObjProdOrder]): Boolean = {
    if (prodOrder.filter(c => c.hasFga == "Y" && c.buildType == "BUILDTOSTOCK").size == prodOrder.size)
      true
    else
      false
  }

  def isGolfOTMEnabled(salesOrderRef: String,eventId: Long): Boolean = {
    if (cmnapiUtil.getSOAttribute(salesOrderRef, "IsLgsSvcEnabled",eventId).equalsIgnoreCase("N"))
      true
    else
      false
  }

  def isSSCNotIn(sol: ArrayBuffer[ObjSalesOrderLine]): Boolean = {
    var paramList = cmndbUtil.getParameterValue("NON FGA SKU", "BACKLOG2_OFS_DTL_VIEW", "UPB_DATA")

    if (paramList.size > 0) {
      if (sol.filter(c => paramList.contains(c.ssc)).size > 0) {
        return false
      }
    }
    true
  }

  def isInvntoryTransfer(soRef: String,eventId: Long): Boolean = {
    if (cmnapiUtil.getSOAttribute(soRef, "INVENTORYTRANSFER",eventId).equalsIgnoreCase("TRUE")) {
      true
    }
    else
      false
  }

  def is3PLRestrictionEnabled(soRef: String,eventId: Long): Boolean = {
    var attrName = cmndbUtil.getParameterValue("READY_STOCK_3PL_RESTRICTION")
    var attrValue = cmndbUtil.getParameterValue("READY_STOCK_3PL_RESTRICTION_ENABLED")
    if (attrName.size > 0) {
      attrName.foreach(attr => {
        if (attrValue.contains(cmnapiUtil.getSOAttribute(soRef, attr,eventId)) == true) {
          return true
        }
      })
    }
    false
  }
  def IS_3PL_LOGIC_ENABLED(region_3PL: String): Boolean ={

    var check_3PL = cmndbUtil.getParameterValue(region_3PL)

    if (check_3PL.size>0 && check_3PL.contains("Y")){

      true

    }

    else

      false

  }
}
