package code

import java.text.SimpleDateFormat
import com.typesafe.config.{Config, ConfigFactory}
import entities.{ASNEntity, CommonEntity, IncomingDragOrder, IncomingProdOrder, IncomingSalesOrder, jsonent}
import core.CommonUtils
import constant.PropertiesLoader
import java.util.{Calendar, Optional, Properties}
import net.liftweb.json.JsonParser.ParseException
import net.liftweb.json.{DefaultFormats, MappingException, Serialization, parse}
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka.{CommitterSettings, ConsumerSettings}
import akka.stream.RestartSettings
import akka.stream.scaladsl.{RestartSource, Sink}
import Irepository.ICommonEntityDAO
import apiCode.ApiHandler
import code.ProcessBacklog.{ProcessBacklogActor, commonUtil, dslogger, fileActorBehavior, inputFormat, logger}
import code.ReadWriteData.getCommonEntity
import config.{IConnection, MyBatisConnection}
import repository.CommonEntityDAO
import com.google.inject.Inject
import org.apache.ibatis.exceptions.PersistenceException
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import spray.json._
import scala.concurrent.duration.DurationInt

trait SOJsonProtocol extends DefaultJsonProtocol {
  implicit val orderFormat = jsonFormat16(CommonEntity)
}

object ProcessBacklog {
  implicit val formats = DefaultFormats

  @Inject var sessionFac: IConnection = MyBatisConnection
  @Inject var ws: ICommonEntityDAO = new CommonEntityDAO(sessionFac)

  val restartSettings = RestartSettings(minBackoff = 3.seconds, maxBackoff = 30.seconds, randomFactor = 0.2)

  val config: Config = ConfigFactory.load()
  val logger: Logger = getLogger(this.getClass)
  val dslogger: LoggingUtil = new LoggingUtil(ExperienceEnum.MTRC, "UPP_Backlog_Engine", "UPP_Backlog_Engine_KafkaReadWrite", "UPPBE", "UPPBE_ControlM_JobName", "1.0-SNAPSHOT", "9999999999", "Dev")
  val commonUtil = new CommonUtils(ws)
  val inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  val topic = PropertiesLoader.INGRESSTopicName
  val groupId = PropertiesLoader.ConsumerGroup
  val bootstrap = PropertiesLoader.kafkaBootStrap
  val committerSettings = CommitterSettings(config.getConfig("akka.kafka.committer"))
  val commiterFlow = Committer.flow(committerSettings)
  val kafkaConsumerSettings = ConsumerSettings(config.getConfig("akka.kafka.consumer"),
    new StringDeserializer, new StringDeserializer)
    .withBootstrapServers(bootstrap)
    .withGroupId(groupId)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  trait ProcessBacklog
  case class ProcessBacklogActor() extends ProcessBacklog

  val fileActorBehavior: Behavior[ProcessBacklogActor] =
    Behaviors.setup {
      context =>
        implicit val ProcessBacklogActorSystem = context.system

/*        logger.info(PropertiesLoader.localjaas + PropertiesLoader.localKrb5 + PropertiesLoader.localTrustStore)
        System.setProperty("java.security.auth.login.config", PropertiesLoader.localjaas)
        System.setProperty("java.security.krb5.conf", PropertiesLoader.localKrb5)*/

        logger.info(PropertiesLoader.clusterjaas + PropertiesLoader.clusterKrb5 + PropertiesLoader.clusterTrustStore)
        System.setProperty("java.security.auth.login.config", PropertiesLoader.clusterjaas)
        System.setProperty("java.security.krb5.conf", PropertiesLoader.clusterKrb5)

        logger.info("topic=%s for groupId=%s and bootstrap=%s".format(topic, groupId, bootstrap))
        logger.info("Before consuming kafka message")
        import akka.kafka.Subscriptions
        import akka.stream.{ActorAttributes, Supervision}

        Behaviors.receiveMessage[ProcessBacklogActor] {
          case ProcessBacklogActor() =>
            logger.info("Inside process Backlog Actor")
            val supervisionDecider: Supervision.Decider = {
              case e: RuntimeException =>
                logger.warn(e.printStackTrace().toString)
                Supervision.Resume

              case e =>
                logger.warn(e.printStackTrace().toString)
                Supervision.Resume
            }

            RestartSource
              .onFailuresWithBackoff(restartSettings) { () =>
                Consumer.committableSource(kafkaConsumerSettings, Subscriptions.topics(topic))
                  .map { msg =>
                    logger.info(msg.record.value())
                    logger.info(msg.toString)
                    if (!msg.record.value().isEmpty) {
                      sendDataToMaster(msg.record.value)
                    }
                    msg.committableOffset
                  }
                  .withAttributes(ActorAttributes.supervisionStrategy(supervisionDecider))
                  .via(Committer.flow(committerSettings))
                  .withAttributes(ActorAttributes.supervisionStrategy(supervisionDecider))
              }.to(Sink.ignore).run()
            Behaviors.same
        }
    }

  def sendDataToMaster(jsonString: String): Unit = {
    logger.info("Sending Json String to Master for Further Processing...")

    val commonEntity = getCommonEntity(jsonString)
    if (commonEntity != null && PropertiesLoader.caseClassSchemaName != "ASNEntity") {
      logger.info(commonEntity.toString)
      if (callOptimizer(commonEntity)) {
        logger.info("commonEntity.toJson " + Serialization.write(commonEntity) + " event_id " + commonEntity.event_ID)
        ApiHandler.callOrchestrator(commonEntity)
      }
    }
  }

  def callOptimizer(message: CommonEntity): Boolean = {
    try {
      val optResponse = commonUtil.getOptimizer(PropertiesLoader.OptimizerUrl, message)
      commonUtil.writeDatatoCommon(optResponse)
      logger.info("Event Optimized status is " + optResponse.status + " for event_id " + message.event_ID)

      if (optResponse.status.equalsIgnoreCase("Optimized")) {
        return false
      }
      if (optResponse.status.equalsIgnoreCase("Retry")) {
        val updatedEntity = optResponse.copy(sys_LAST_MODIFIED_DATE = inputFormat.format(Calendar.getInstance.getTime))
        commonUtil.pushMessage(updatedEntity)
        return false
      }
      true
    }
    catch {
      case ex: ParseException =>
        logger.error("CallOptimizer parseException " + message.event_ID, ex)
        val updatedEntity = message.copy(status = "Retry", sub_STATUS = "NA",
          sys_LAST_MODIFIED_DATE = inputFormat.format(Calendar.getInstance.getTime),
          comments = "Optimizer failed and pushed to retry")
        commonUtil.writeDatatoCommon(updatedEntity)
        commonUtil.pushMessage(updatedEntity)
        false
      case ex: PersistenceException => {
        if (ex.getCause.toString.contains("SQLIntegrityConstraintViolationException")) {
          logger.error("SQLException while inserting into table " + message.event_ID, ex)
          val eId = commonUtil.getEventId
          val updatedEntity = message.copy(event_ID = eId, status = "Retry", sub_STATUS = "NA",
            sys_LAST_MODIFIED_DATE = inputFormat.format(Calendar.getInstance.getTime),
            comments = "inserting into UPB_events failed and pushed to retry")
          commonUtil.writeDatatoCommon(updatedEntity)
          commonUtil.pushMessage(updatedEntity)
        }
        else {
          logger.error("SQLException while inserting into UPB_EVENTS " + message.event_ID, ex)
          val updatedEntity = message.copy(status = "Retry", sub_STATUS = "NA",
            sys_LAST_MODIFIED_DATE = inputFormat.format(Calendar.getInstance.getTime),
            comments = "inserting into UPB_events failed and pushed to retry")
          commonUtil.pushMessage(updatedEntity)
        }
        false
      }
      case ex =>
        logger.error("callOptimizer " + message.event_ID, ex)
        val updatedEntity = message.copy(status = "Retry", sub_STATUS = "NA", comments = "callOptimizer failed",
          sys_LAST_MODIFIED_DATE = inputFormat.format(Calendar.getInstance.getTime))
        commonUtil.pushMessage(updatedEntity)
        false
    }
  }
}

object ReadWriteData {
  implicit val fileTypedActor = ActorSystem(fileActorBehavior, "RetryBacklog")
  val execCon = fileTypedActor.executionContext
  implicit val formats = DefaultFormats

  def main(args: Array[String]): Unit = {
    logger.info("ReadWrite Main")
    fileTypedActor ! ProcessBacklogActor()
  }

  def getCommonEntity(message: String): CommonEntity = {
    var cmn = new CommonEntity("", "", "", 0,
      0, "", "", "", 0, "", "", "", "", "", 0, "")
    try {
      val jsonData = parse(message)
      if (PropertiesLoader.caseClassSchemaName == "IncomingSalesOrder") {
        val ifi = jsonData.extract[IncomingSalesOrder]
        var region = ifi.after.REGION
        /*if(ifi.after.REGION == null  ||  ifi.after.REGION.isEmpty)
          region = commonUtil.getRegion(ifi.after.BUID)*/

        cmn = CommonEntity(ifi.after.DOMS_STATUS, region, "SO", 0,
          ifi.after.BUID, ifi.after.SALES_ORDER_ID, "", "", 0, "",
          "", ifi.after.CREATE_DATE, "", ifi.table, 0, "false")
      }
      else if (PropertiesLoader.caseClassSchemaName == "IncomingProdOrder") {
        val ifi = jsonData.extract[IncomingProdOrder]
        var region = ifi.after.REGION
       /* if(ifi.after.REGION == null  ||  ifi.after.REGION.isEmpty)
          region = commonUtil.getRegion(ifi.after.BUID)*/

        cmn = CommonEntity(ifi.after.STATUS_CODE, region, "PO", 0,
          ifi.after.BUID, ifi.after.SALES_ORDER_ID, "", "", 0, "",
          "", ifi.after.CREATE_DATE, ifi.after.PROD_ORDER_NUM, ifi.table, 0, "false")
      }
      else if (PropertiesLoader.caseClassSchemaName == "IncomingDragOrder") {

        val ifi = jsonData.extract[IncomingDragOrder]
        cmn = CommonEntity("", "APJ", "DO", 0,
          ifi.after.BUID, ifi.after.ORDER_NUM, "", "", 0, "",
          "", ifi.after.MODIFIED_DATE, "", ifi.table, 0, "false")
      }
      else if (PropertiesLoader.caseClassSchemaName == "ASNEntity") {
        var asnobj = ASNEntity("", "", "", "",
          "", "", "", "", "", "", "")
        val ifi = jsonData.extract[jsonent]
        if (ifi.EventID == "3150" || ifi.EventID == "4008") {
          val comm_ent = ifi.Attributes
          var eve_nam = ""
          var stat = ""
          var chann = ""
          var Datasourc = ""
          var fina_asn = ""
          //var Tracid_nam = ""
          var Reg = ""
          var shifro = ""
          var shito = ""
          var ven_nam = ""
          //val eventId = commonUtil.getEventId()
          comm_ent.foreach(attr =>
            if (attr.Name == "EventName") {
              eve_nam = attr.Value
            }
            else if (attr.Name == "State") {
              stat = attr.Value
            }
            else if (attr.Name == "Channel") {
              chann = attr.Value
            }
            else if (attr.Name == "DataSource") {
              Datasourc = attr.Value
            }
            else if (attr.Name == "FinalASN") {
              fina_asn = attr.Value
            }
            /*else if (attr.Name == "TraceId") {
              Tracid_nam = attr.Value
            }*/
            else if (attr.Name == "Region") {
              Reg = attr.Value
            }
            else if (attr.Name == "ShipFrom") {
              shifro = attr.Value
            }
            else if (attr.Name == "ShipTo") {
              shito = attr.Value
            }
            else if (attr.Name == "VendorName") {
              ven_nam = attr.Value
            })
          print(stat, chann, Datasourc, fina_asn, Reg, shifro, shito, ven_nam, eve_nam)
          asnobj = ASNEntity(ifi.EntityValue(0), ifi.EventID, eve_nam, stat, chann, Datasourc, fina_asn, Reg, shifro, shito, ven_nam)
          commonUtil.writeDatatoASN(asnobj)
        }
      }
      val eventId = commonUtil.getEventId()
      cmn.copy(event_ID = eventId)
    }
    catch {
      case ex: PersistenceException => {
        logger.error("SQLException while generating event id", ex)
        val updatedEntity = cmn.copy(status = "Retry", sub_STATUS = "NA",
          sys_LAST_MODIFIED_DATE = inputFormat.format(Calendar.getInstance.getTime),
          comments = "failed to generate eventId and pushed to retry")
        commonUtil.pushMessage(updatedEntity)
        null
      }
      case ex: MappingException => {
        logger.error("Error while converting into Object " + message, ex)
        null
      }
      case ex: ParseException => {
        logger.error("Error while parsing the message" + message, ex)
        null
      }
      case ex => {
        dslogger.error("Kafka stream structure does not match with the case class defined (EVENT_ID: )" + ex.printStackTrace(), ex, "")
        logger.error("Kafka stream structure does not match with the case class defined (EVENT_ID:)", ex)
        null
      }
    }
  }
}