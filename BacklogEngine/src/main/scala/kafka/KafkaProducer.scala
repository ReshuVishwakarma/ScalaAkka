package kafka

import java.text.SimpleDateFormat
import java.util.{Calendar, Properties}
import akka.kafka.ProducerSettings
import Irepository.ICommonEntityDAO
import code.RuleEngine.{config, inme, logger}
import config.{IConnection, MyBatisConnection}
import constant.PropertiesLoader
import core.CommonDBUtil
import entities.CommonEntity
import kafka.KafkProperties.{inputFormat, kafkaProducer}
import repository.CommonEntityDAO
import com.google.inject.Inject
import net.liftweb.json.DefaultFormats
import net.liftweb.json.Serialization.write
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

object KafkProperties {
  val inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  val props = new Properties()

/*System.setProperty("java.security.auth.login.config", PropertiesLoader.localjaas)
System.setProperty("java.security.krb5.conf", PropertiesLoader.localKrb5)*/

logger.info( PropertiesLoader.clusterjaas +  PropertiesLoader.clusterKrb5 + PropertiesLoader.clusterTrustStore)
System.setProperty("java.security.auth.login.config", PropertiesLoader.clusterjaas)
System.setProperty("java.security.krb5.conf", PropertiesLoader.clusterKrb5)

val producerSettings =
  ProducerSettings(config.getConfig("akka.kafka.producer"), new StringSerializer, new StringSerializer)
    .withBootstrapServers(PropertiesLoader.kafkaBootStrap)
val kafkaProducer = producerSettings.createKafkaProducer()
}

class KafkaProducer {

@Inject var sessionFac:IConnection = MyBatisConnection
@Inject var ws: ICommonEntityDAO = new CommonEntityDAO(sessionFac)
val cmndbUtil = new CommonDBUtil(ws,inme)

def pushMessagetoRetry(message: CommonEntity,comment:String): Unit ={
  implicit val formats = DefaultFormats

  cmndbUtil.updateStatus(message.event_ID,"Retry","NA",comment,0)
  val updatedMsg = message.copy(sys_LAST_MODIFIED_DATE = inputFormat.format(Calendar.getInstance.getTime),status = "Retry",parent_EVENT_ID = 0)
  logger.info("Event_Id- "+updatedMsg.event_ID+"|| Retry message "+updatedMsg)

  val msg =  write(updatedMsg).replaceAll("null","\"\"")
  val record = new ProducerRecord(PropertiesLoader.INGRESSTopicName,message.event_ID.toString,msg)
  try {
    val metadata = kafkaProducer.send(record).get()
    logger.info(metadata.toString + " for event id " + message.event_ID)
  }
  catch {
    case ex => logger.error("Event_Id- "+updatedMsg.event_ID+"|| error while pushing to retry" + ex.getCause)
  }
}
}
