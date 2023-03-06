package kafka

import akka.kafka.ProducerSettings
import code.ProcessBacklog.logger
import constant.PropertiesLoader
import kafka.KafkaProducerObj.{kafkaProducer}
import com.typesafe.config.{Config, ConfigFactory}
import net.liftweb.json.DefaultFormats
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

object KafkaProducerObj {
  val config: Config = ConfigFactory.load()
  val producerSettings =
    ProducerSettings(config.getConfig("akka.kafka.producer"), new StringSerializer, new StringSerializer)
      .withBootstrapServers(PropertiesLoader.kafkaBootStrap)
  val kafkaProducer =  producerSettings.createKafkaProducer()
}
class KafkaProducer {

  def pushMessage(key: String, message: String): Unit = {

    implicit val formats = DefaultFormats
    val msg =  message.replaceAll("null","\"\"")
    val record = new ProducerRecord(PropertiesLoader.retrytopic, key, msg)
    try {
      val metadata = kafkaProducer.send(record).get()
      logger.info(metadata.toString + "for event id " + key )
    }
    catch {
      case ex =>
        logger.error("pushMessage", ex)
    }
  }

}
