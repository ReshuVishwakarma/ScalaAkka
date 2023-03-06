package constant

import java.util.Properties

object PropertiesLoader {

  // var logger: LoggingUtil = null
  val connectionParam = new Properties

  val app = connectionParam.getProperty("app")

  connectionParam.load(getClass().getResourceAsStream("/fve.properties"))


  val environment = connectionParam.getProperty("environment")

  //DB Properties
  val url = connectionParam.getProperty("url")
  val userName = connectionParam.getProperty("user")
  val password = connectionParam.getProperty("password")

  //Kafka Properties
  val kafkaBootStrap = connectionParam.getProperty("kafka.bootstrap.servers")
  val localTrustStore = connectionParam.getProperty("local_truststore_file")
  val clusterTrustStore = connectionParam.getProperty("truststore_file_path")
  val localKeyStore = connectionParam.getProperty("local_keystore_file")
  val clusterKeyStore = connectionParam.getProperty("keystore_file_path")
  val localKrb5 = connectionParam.getProperty("local_krb5_file")
  val clusterKrb5 = connectionParam.getProperty("krb5_file_path")
  val localjaas = connectionParam.getProperty("local_jass_conf")
  val clusterjaas = connectionParam.getProperty("jaas_conf_path")
  val kafkaSecurityProtocol = connectionParam.getProperty("security_protocol")
  val saslMechanism = connectionParam.getProperty("sasl_mechanism")
  val sslProtocol = connectionParam.getProperty("ssl_enabled_protocols")
  val INGRESSTopicName = connectionParam.getProperty("ingress_topic_name")
  val ConsumerGroup = connectionParam.getProperty("ingress_cn_grp_name")
  val TruststorePassword = connectionParam.getProperty("truststore_password")
  val retrytopic = connectionParam.getProperty("retry_topic")
  val caseClassSchemaName = connectionParam.getProperty("caseClassSchemaName")
  val serviceName = connectionParam.getProperty("serviceName")
  val OptimizerUrl = connectionParam.getProperty("optimizerUrl")
  val apiHostName = connectionParam.getProperty("API_HOST_NAME")

  def loadProperty(param: String): String = {
    connectionParam.getProperty(param)
  }
}
