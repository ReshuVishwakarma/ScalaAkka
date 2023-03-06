package core

import net.liftweb.json.{DefaultFormats, parse}
import java.util.Calendar
import net.liftweb.json.Serialization.write
import Irepository.ICommonEntityDAO
import apiCode.ApiHandler
import code.ProcessBacklog.{dslogger, logger}
import constant.UPPBEConstants
import entities.{ASNEntity, CommonEntity, EventId}
import kafka.KafkaProducer
import com.google.inject.Inject

class CommonUtils  @Inject()( repository : ICommonEntityDAO ){

  final var repos: ICommonEntityDAO = _

  {
    this.repos = repository
  }

  implicit val formats = DefaultFormats


  // Inserting stream values into UPB_EVENTS for audit and further processing
  def writeDatatoCommon(dataMessage: CommonEntity): Boolean = {
    try {
      repository.insertItem(UPPBEConstants.INSERT_COMMON_ENTITY, dataMessage)
    } catch {
      case ex => {
        dslogger.error("SQLException while inserting into UPB_EVENTS (EVENT_ID:" + dataMessage.event_ID + ")" + ex.printStackTrace(), ex, "")
        logger.error("SQLException while inserting into UPB_EVENTS: (EVENT_ID:" + dataMessage.event_ID + ")" , ex)
        ex.getMessage
        throw ex
      }
    }
  }

  def UpdateCommon(dataMessage: CommonEntity): Unit = {
    try {
      repository.updateItem(UPPBEConstants.UPDATE_COMMON_ENTITY, dataMessage)
      logger.info("updated Event Status completed for event_id " + dataMessage.event_ID)
    } catch {
      case ex => {
        dslogger.error("SQLException while inserting into UPB_EVENTS (EVENT_ID:" + dataMessage.event_ID + ")" + ex.printStackTrace(), ex, "")
        logger.error("SQLException while inserting into UPB_EVENTS: (EVENT_ID:" + dataMessage.event_ID + ")" , ex)
        ex.getMessage
        throw ex
      }
    }
  }


  def getOptimizer(msEndPoint: String, message: CommonEntity): CommonEntity = {
    try {
      val msg = write(message)
      logger.info(Calendar.getInstance.getTime + " .... Get Optimizer .... \nAPI URL- " + msEndPoint +
        "\nAPI input parameter passed- " + msg)
      val resp = ApiHandler.postRequest(msEndPoint, msg)
      val responseData = (parse(resp) \ "data").extract[CommonEntity]
      responseData
    }
    catch {
      case ex =>
        logger.error("getOptimizer " , ex)
        throw ex
    }
  }

  // Function for generating unique event_id for each stream
  def getEventId(): Long = {
    try {
      val eventId = repository.getEventId(UPPBEConstants.GET_EVENT_ID).asInstanceOf[EventId]
      eventId.eventId
    } catch {
      case ex => {
        dslogger.error("Exception while generating event_id:  " +  ex.printStackTrace(), ex,"")
        logger.error("Exception while generating event_id: " , ex)
        ex.getMessage
        throw ex
      }
    }
  }

  def getRegion(buid: Int): String = {
    try {
      repository.getDataWithParam(UPPBEConstants.GET_REGION, buid).asInstanceOf[String]
    } catch {
      case ex => {
        dslogger.error("Exception while getting region  " +  ex.printStackTrace(), ex,"")
        logger.error("Exception while getting region " , ex)
        ex.getMessage
        throw ex
      }
    }
  }

  def pushMessage(updatedEntity : CommonEntity): Unit =
  {
    try {
      val kafkaProducer = new KafkaProducer()
      kafkaProducer.pushMessage(updatedEntity.event_ID.toString, write(updatedEntity))
    }
    catch {
      case ex => {
        dslogger.error("Exception while generating event_id:  " +  ex.printStackTrace(), ex,"")
        logger.error("Exception while generating event_id: " , ex)
        ex.getMessage
        throw ex
      }
    }
  }

  def writeDatatoASN(dataASN: ASNEntity): Boolean = {
    try {
      repository.insertItem(UPPBEConstants.INSERT_ASN_DATA, dataASN)
    } catch {
      case ex => {
        dslogger.error("SQLException while inserting into UPB_EVENTS" + ex.printStackTrace(), ex, "")
        logger.error("SQLException while inserting into UPB_EVENTS" , ex)
        ex.getMessage
        throw ex
      }
    }
  }
}