package common

import Irepository.{ICommonEntityDAO}
import code.RuleEngine.logger
import com.google.inject.Inject



class DataPersistHandler @Inject() ( repository : ICommonEntityDAO ) {
  final var repos: ICommonEntityDAO = _

  {
    this.repos = repository
  }

  // Inserting stream values into UPB_EVENTS for audit and further processing
  def writeDatatoTable[T](tableName: String, dataMessage: T): Boolean = {
    try {
      if(dataMessage != null)
        repository.insertItem(tableName, dataMessage)
      else true
    } catch {
      case ex => {
        //dslogger.error("SQLException while inserting into UPB_EVENTS (EVENT_ID:" + dataMessage.event_ID + ")" + ex.printStackTrace(), ex, "")
        logger.error("SQLException while inserting into " + tableName + ex.printStackTrace)
        //false
        throw ex
      }
    }
  }

  def writeDatatoTable[T](tableName: String, dataMessage: java.util.List[T]): Boolean = {
    try {
      if(dataMessage != null && dataMessage.size() > 0)
        repository.insertItem(tableName, dataMessage)
      else true
    } catch {
      case ex => {
        //dslogger.error("SQLException while inserting into UPB_EVENTS (EVENT_ID:" + dataMessage.event_ID + ")" + ex.printStackTrace(), ex, "")
        logger.error("SQLException while inserting into " + tableName + ex.printStackTrace)
        //false
        throw ex
      }
    }
  }
}
