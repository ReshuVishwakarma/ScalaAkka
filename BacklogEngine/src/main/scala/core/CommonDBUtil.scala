package core

import java.util.List
import Irepository.{ICommonEntityDAO, IInMemoryDataEntityDAO}
import code.RuleEngine.logger
import constant.UPPBEConstants
import entities.{CommonEntity, CommonEntityDB, DomsStatus, EventId, ParameterEntity, StrEntity}
import com.google.inject.Inject

import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

class CommonDBUtil @Inject()( repository : ICommonEntityDAO,
                              inmemoryrepo : IInMemoryDataEntityDAO) {

  final var cmnentity: ICommonEntityDAO = _
  final var inmemoryentity : IInMemoryDataEntityDAO =_

  {
    this.cmnentity = repository
    this.inmemoryentity = inmemoryrepo
  }


  def getfulfillmentChannel(region:String): ArrayBuffer[String] ={
    try {
      val lst = new ArrayBuffer[String]()
      val fflist =   inmemoryentity.getItemList(UPPBEConstants.GET_FULFILLMENT_CHANNELS,new StrEntity("%" + region + "%")).asInstanceOf[List[StrEntity]]

      fflist.forEach(
        item =>
          lst.append(new String(item.value))
      )
      lst
    } catch {
      case ex => {
        logger.error("SQLException in Writing Status: " + ex.getCause + ex.getMessage + ex.printStackTrace)
        throw ex
      }
    }
  }

  //Added for checks through look-up table
  def getParameterValue(parameterName: String, parameterMethod: String = "", parameterClass: String = ""): ArrayBuffer[String] = {
    try {
      val list = new ArrayBuffer[String]
      val plist =   inmemoryentity.getItemList(UPPBEConstants.GET_PARAMETER_LIST,
        new ParameterEntity(parameterName,parameterMethod,parameterClass)).asInstanceOf[List[StrEntity]]

      plist.forEach(
        item =>
          list.append(new String(item.value))
      )
      list
    }
    catch {
      case ex => {
        //logger.error(" Could not fetch values from look-up table " + ExceptionUtils.getStackTrace(ex), ex, "")
        logger.error(" Could not fetch values from look-up table " + ex.getCause + ex.printStackTrace)
        ex.getMessage
        throw ex
      }
    }
  }

  //For updating UPB_EVENTS table in case of checks failure/ empty result set from API
  def updateStatus(eventId: Long, status: String, substatus: String, comment: String,parentevntId : Int): Unit = {
    logger.info("Event_Id- " + eventId + "|| Updating events table  " + comment)
    try {
      inmemoryentity.UpdateTable(UPPBEConstants.UPDATE_COMMON_ENTITY, CommonEntityDB("", "", "", eventId,
        0, "", status, comment, parentevntId, "",
        substatus, "", "", "", 0,"",
        System.getenv("HOSTNAME")))
    }
    catch {
      case ex => {
        //logger.error(" Could not Update UPB_EVENTS table " + ExceptionUtils.getStackTrace(ex), ex, "")
        logger.error("Event_Id- " + eventId + "|| Could not Update UPB_EVENTS table " + ex.printStackTrace)
        ex.getMessage
        throw ex
      }
    }
  }



  def getMessage(eventId : Long): CommonEntity = {
    try {
      cmnentity.getItem(UPPBEConstants.GET_COMMON_ENTITY, new EventId(eventId)).asInstanceOf[CommonEntity]
    } catch {
      case ex => {
        //dslogger.error("SQLException while inserting into UPB_EVENTS (EVENT_ID:" + dataMessage.event_ID + ")" + ex.printStackTrace(), ex, "")
        logger.error(" Event_id- "+eventId+" || SQLException while getting message " + ex.printStackTrace)
        throw ex
      }
    }
  }

  def purgeInMemData(eventid: Long) = {
    try {
      //val tblist = inmemoryentity.getDataList(UPPBEConstants.GET_IN_MEMORY_TABLES).asInstanceOf[List[StrEntity]]
      //logger.info(tblist.toString)
      //val tblList = convertDeleteStatment(tblist)
      inmemoryentity.deleteTableData(UPPBEConstants.DELETE_TABLE_DATA,eventid)
    } catch {
      case ex => {
        logger.error("Event_Id- "+eventid+"|| Could not purge records" + ex.getCause)
        //logger.error(" Could not load purge records" + ExceptionUtils.getStackTrace(ex), ex, "")
        ex.getMessage
      }
    }
  }

  def status_chk_for_purge(status:String,region:String): String= {
    try {
      val resp = cmnentity.getItem(UPPBEConstants.GET_DOMS_STATUS,new DomsStatus(status,region)).asInstanceOf[StrEntity]
      if(resp != null) resp.value else ""
    }
    catch {
      case ex => {
        logger.error("Could not check status for purge records" + ex.getCause)
        //logger.error(" Could not check status for purge" + ExceptionUtils.getStackTrace(ex), ex, "")
        ex.getMessage
      }
    }
  }

  def convertDeleteStatment(lst : List[StrEntity]): List[StrEntity] ={
    val tblLst = new ListBuffer[StrEntity]()
    lst.forEach(
      tbl =>
        tblLst += new StrEntity( "Delete from " + tbl.value + "where event_id = 15687")
    )
    tblLst.asJava
  }
}
