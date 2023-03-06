package apiCode

import java.security.{KeyStore, KeyStoreException, NoSuchAlgorithmException, SecureRandom}
import java.util.{Calendar, Optional}
import akka.http.scaladsl.{ConnectionContext, Http}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.RestartSettings
import akka.stream.scaladsl.{Flow, RestartSource, Sink, Source}
import code.ProcessBacklog.{commonUtil, inputFormat, logger}
import code.ReadWriteData.{execCon, fileTypedActor}
import constant.PropertiesLoader
import constant.PropertiesLoader.apiHostName
import entities.CommonEntity
import javax.net.ssl.{SSLContext, TrustManagerFactory}
import net.liftweb.json.{DefaultFormats, Serialization}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

object ApiHandler {
  implicit val formats = DefaultFormats

  var connFlow: Flow[(HttpRequest, Int), (Try[HttpResponse], Int), Http.HostConnectionPool] = _
  var pool: Flow[(HttpRequest, Int), (Try[HttpResponse], Int), Http.HostConnectionPool] = _
  val https = Http.get(fileTypedActor)

  val trustKeyStore = createKeyStore("JKS", "kafka.client.truststore.jks", "kafkatrust")
  val trustManagerFactory = createTrustManagerFactory("SunX509", trustKeyStore)
  val sslContext = SSLContext.getInstance("TLS")
  sslContext.init(null, trustManagerFactory.getTrustManagers, new SecureRandom())
  val connectionContext = ConnectionContext.httpsClient(sslContext)
  https.setDefaultClientHttpsContext(connectionContext)
  pool = https.cachedHostConnectionPoolHttps(apiHostName)
  connFlow = Http().cachedHostConnectionPool(PropertiesLoader.serviceName, 8082)

  def postRequest(url: String, json: String): String = {
    val start : Long  = System.currentTimeMillis()
    try {
      val request = HttpRequest.apply().withMethod(HttpMethods.POST).
        withEntity(HttpEntity(ContentTypes.`application/json`, json)).withUri(url)

      val responseFuture: Future[HttpResponse] =
        RestartSource.onFailuresWithBackoff(RestartSettings(5.second, 10.seconds, 0.1))(() =>
          Source.single(request -> 42))
          .via(pool)
          .map(_._1.get)
          .runWith(Sink.head)

      val resp = Await.result(responseFuture, 20.seconds)
      logger.info(" API call status " + resp.status )
      val str = Await.result(Unmarshal(resp.entity).to[String], 20.seconds)

      if (resp.status != StatusCodes.OK || str == null) {
        logger.info("Post request response " + str )
        val end : Long = System.currentTimeMillis()
        logger.info("Webservice took " + (end  - start))
        return null
      }
      val end : Long = System.currentTimeMillis()
      logger.info("Webservice took " + (end - start))
      str
    }
    catch {
      case ex=>
        logger.error("post api call exception " ,ex)
        throw ex
    }
  }

  def callOrchestrator(commonEntity: CommonEntity): Unit = {

    logger.info("Orchestrator Service " + PropertiesLoader.serviceName + " for Event_Id " + commonEntity.event_ID)
    val start = System.currentTimeMillis()
    val request = HttpRequest.apply().withMethod(HttpMethods.POST).
      withEntity(HttpEntity(ContentTypes.`application/json`, Serialization.write(commonEntity).replaceAll("null", "\"\""))).withUri("/order/process")

    val responseFuture: Future[HttpResponse] =
      Source.single(request -> 42)
        .via(connFlow)
        .map(_._1.get)
        .runWith(Sink.head)

    responseFuture.onComplete {
      case Success(resp) => {
        val end = System.currentTimeMillis()
        logger.info("Orchestrtor call took " + (end - start))
        if (resp.status == StatusCodes.OK)
          logger.info(resp.toString + " event_id " + commonEntity.event_ID)
        else {
          logger.error(resp.toString + " event_id " + commonEntity.event_ID)
          val updatedEntity = commonEntity.copy(status = "Retry", comments = "Orchestrator Http Call failed in retry",
            sys_LAST_MODIFIED_DATE = inputFormat.format(Calendar.getInstance.getTime))
          commonUtil.UpdateCommon(updatedEntity)
          commonUtil.pushMessage(updatedEntity)
        }
      }
      case Failure(resp) => {
        val end = System.currentTimeMillis()
        logger.info("Orchestrtor call took " + (end - start))
        logger.error(resp.toString + " event_id " + commonEntity.event_ID)
        val updatedEntity = commonEntity.copy(status = "Retry", sub_STATUS = "NA", comments = "Orchestrator Http Call failed in retry",
          sys_LAST_MODIFIED_DATE = inputFormat.format(Calendar.getInstance.getTime))
        commonUtil.UpdateCommon(updatedEntity)
        commonUtil.pushMessage(updatedEntity)
      }
    }(execCon)
  }

  def createKeyStore(`type`: String, resourcePath: String, password: String) = try {
    val certificate = Optional.ofNullable(classOf[Nothing].getClassLoader.getResourceAsStream(resourcePath)).orElseThrow(() => new IllegalArgumentException("Certificate file not found: " + resourcePath))
    val keyStore = KeyStore.getInstance(`type`)
    keyStore.load(certificate, password.toCharArray)
    keyStore
  } catch {
    case ex =>
      throw new IllegalArgumentException("Error creating KeyStore", ex)
  }

  def createTrustManagerFactory(name: String, trustKeyStore: KeyStore) = try {
    val trustManagerFactory = TrustManagerFactory.getInstance(name)
    trustManagerFactory.init(trustKeyStore)
    trustManagerFactory
  } catch {
    case e@(_: NoSuchAlgorithmException | _: KeyStoreException) =>
      throw new IllegalArgumentException("Error creating TrustManagerFactory", e)
  }
}
