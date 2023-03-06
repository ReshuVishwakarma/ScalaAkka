package constant

import java.util.Properties

object PropertiesLoader {

  // var logger: LoggingUtil =
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
  val truststore_password = connectionParam.getProperty("truststore_password")

  val myBatisConfiguration = connectionParam.getProperty("MY_BATIS_CINFIGURATION")
  val apiHostName = connectionParam.getProperty("API_HOST_NAME")
  //API Url APJ

  val APJSalesOrderUrl = connectionParam.getProperty("APJ_SalesOrderUrl")
  val APJ_ProdOrderOPRUrl = connectionParam.getProperty("APJ_ProdOrderOPRUrl")
  val APJ_ProdOrderUrl = connectionParam.getProperty("APJ_ProdOrderUrl")
  val APJ_SOLOPRUrl = connectionParam.getProperty("APJ_SOLOPRUrl")
  val APJ_SOLUrl = connectionParam.getProperty("APJ_SOLUrl")
  val SOAttributeUrl = connectionParam.getProperty("SOAttributeUrl")

  val APJ_LineSKUUrl = connectionParam.getProperty("APJ_LineSKUUrl")
  val APJ_LineSKUOprUrl = connectionParam.getProperty("APJ_LineSKUOprUrl")

  val APJ_LineSKUPartOPRUrl = connectionParam.getProperty("APJ_LineSKUPartOPRUrl")
  val APJ_LineSKUPartUrl = connectionParam.getProperty("APJ_LineSKUPartUrl")

  val APJ_BacklogOPRUrl = connectionParam.getProperty("APJ_BacklogOPRUrl")
  val APJ_BacklogUrl = connectionParam.getProperty("APJ_BacklogUrl")
  val APJ_SO_Attribute_Ref = connectionParam.getProperty("APJ_SO_Attribute_Ref")

  //API Url EMEA
  val EMEASalesOrderUrl = connectionParam.getProperty("EMEASalesOrderUrl")
  val EMEA_ProdOrderOPRUrl = connectionParam.getProperty("EMEA_ProdOrderOPRUrl")
  val EMEA_ProdOrderUrl = connectionParam.getProperty("EMEA_ProdOrderUrl")
  val EMEA_SOLOPRUrl = connectionParam.getProperty("EMEA_SOLOPRUrl")
  val EMEA_SOLUrl = connectionParam.getProperty("EMEA_SOLUrl")

  val EMEA_LineSKUUrl = connectionParam.getProperty("EMEA_LineSKUUrl")
  val EMEA_LineSKUOprUrl = connectionParam.getProperty("EMEA_LineSKUOprUrl")

  val EMEA_LineSKUPartOPRUrl = connectionParam.getProperty("EMEA_LineSKUPartOPRUrl")
  val EMEA_LineSKUPartUrl = connectionParam.getProperty("EMEA_LineSKUPartUrl")
  val EMEA_BacklogOPRUrl=connectionParam.getProperty("EMEA_BacklogOPRUrl")
  val EMEA_BacklogUrl=connectionParam.getProperty("EMEA_BacklogUrl")
  val EMEA_SO_Attribute_Ref = connectionParam.getProperty("EMEA_SO_Attribute_Ref")

  val caseClassSchemaName = connectionParam.getProperty("caseClassSchemaName")

  //API Dragon Url
  val Drag_bklg_url=connectionParam.getProperty("APJ_Drag_bklg_url")
  val Drag_ord_detail_url= connectionParam.getProperty("APJ_Drag_ord_detail")
  val Drag_ord_header_url=connectionParam.getProperty("APJ_Drag_ord_header")
  val Drag_ord_ful_url=connectionParam.getProperty("APJ_Drag_ord_ful_url")

  //API Url EMEA
  val DAOSalesOrderUrl = connectionParam.getProperty("DAOSalesOrderUrl")
  val DAO_ProdOrderOPRUrl = connectionParam.getProperty("DAO_ProdOrderOPRUrl")
  val DAO_ProdOrderUrl = connectionParam.getProperty("DAO_ProdOrderUrl")
  val DAO_SOLOPRUrl = connectionParam.getProperty("DAO_SOLOPRUrl")
  val DAO_SOLUrl = connectionParam.getProperty("DAO_SOLUrl")
  val DAO_ProdOrderUnitUrl = connectionParam.getProperty("DAO_ProdOrderUnitUrl")
  val DAO_SO_Attribute_Ref = connectionParam.getProperty("DAO_SO_Attribute_Ref")

  val DAO_LineSKUUrl = connectionParam.getProperty("DAO_LineSKUUrl")
  val DAO_LineSKUOprUrl = connectionParam.getProperty("DAO_LineSKUOprUrl")

  val DAO_LineSKUPartOPRUrl = connectionParam.getProperty("DAO_LineSKUPartOPRUrl")
  val DAO_LineSKUPartUrl = connectionParam.getProperty("DAO_LineSKUPartUrl")

  val DAO_backlogOFSUrl=connectionParam.getProperty("DAO_backlogOFSUrl")
  val DAO_backlogOPRUrl=connectionParam.getProperty("DAO_backlogOPRUrl")

  val Delete_Backlog_Detail = connectionParam.getProperty("Delete_Backlog_Detail")
  val Delete_Backlog_Header = connectionParam.getProperty("Delete_Backlog_Header")
  val Delete_DRGN_Backlog_Detail = connectionParam.getProperty("Delete_DRGN_Backlog_Detail")
  val Delete_DRGN_Backlog_Header = connectionParam.getProperty("Delete_DRGN_Backlog_Header")

  //Added for 3pl
  val API_3PL=connectionParam.getProperty("API_3PL")
  val Backlog_insert=connectionParam.getProperty("Backlog_insert")
  val IM_Backlog_Insert=connectionParam.getProperty("IM_Backlog_Insert")

  def loadProperty(param: String): String = {
    connectionParam.getProperty(param)
  }

  val testFile = connectionParam.getProperty("test_file_name")
  val testFilePath = connectionParam.getProperty("test_file_path")
  val testAppName = connectionParam.getProperty("test_appname")
}
