package common

import java.security.{KeyStore, KeyStoreException, NoSuchAlgorithmException, SecureRandom}
import java.util.Optional
import akka.http.scaladsl.{ConnectionContext, Http}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.RestartSettings
import akka.stream.scaladsl.{Flow, RestartSource, Sink, Source}
import Irepository.ICommonEntityDAO
import code.Orchestrator.backlogEngineActorsystem
import code.RuleEngine.{inme, logger}
import config.{IConnection, MyBatisConnection}
import constant.PropertiesLoader.apiHostName
import core.CommonDBUtil
import kafka.KafkaProducer
import repository.CommonEntityDAO
import com.google.inject.Inject
import javax.net.ssl.{SSLContext, TrustManagerFactory}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.Try

object AkkaHttp {

  @Inject var sessionFac: IConnection = MyBatisConnection
  @Inject var ws: ICommonEntityDAO = new CommonEntityDAO(sessionFac)

  val cmnutil = new CommonDBUtil(ws, inme)
  val kfkProducer = new KafkaProducer()

  var pool: Flow[(HttpRequest, Int), (Try[HttpResponse], Int), Http.HostConnectionPool] = _

  val trustKeyStore = createKeyStore("JKS", "kafka.client.truststore.jks", "kafkatrust")
  val trustManagerFactory = createTrustManagerFactory("SunX509", trustKeyStore)
  val sslContext = SSLContext.getInstance("TLS")
  sslContext.init(null, trustManagerFactory.getTrustManagers, new SecureRandom())
  val connectionContext = ConnectionContext.httpsClient(sslContext)

  val https = Http.get(backlogEngineActorsystem)

  https.setDefaultClientHttpsContext(connectionContext)
  pool = https.cachedHostConnectionPoolHttps(apiHostName)


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

  def getResponse(url: String, eventId: Long): String = {
    try {
      logger.info(" API call - "+url+"  started for event id " + eventId)
      val request = HttpRequest.apply().withMethod(HttpMethods.GET).withUri(url)
      val responseFuture: Future[HttpResponse] =
        RestartSource.onFailuresWithBackoff(RestartSettings(5.seconds, 10.seconds, 0.1))(() =>
          Source.single(request -> 42))
          .via(pool)
          .map(_._1.get)
          .runWith(Sink.head)

      val resp = Await.result(responseFuture, 30.seconds)
      logger.info(" API - "+url+" call status " + resp.status + " for event id " + eventId)
      val str = Await.result(Unmarshal(resp.entity).to[String], 30.seconds)

      if (resp.status != StatusCodes.OK || resp.entity == null) {
        logger.info("EventId- "+eventId+" || get response for api - "+url+" is " + str)
        return null
      }
      str
    }
    catch {
      case ex => logger.error("EventId- "+eventId+" || Exception During API - "+url+"  call", ex)
        null
    }
  }
  def putRequest(url: String, json: String, eventId: Long): String = {
    try {
      val request = HttpRequest.apply().withMethod(HttpMethods.PUT).
        withEntity(HttpEntity(ContentTypes.`application/json`, json)).withUri(url)

      val responseFuture: Future[HttpResponse] =
        RestartSource.onFailuresWithBackoff(RestartSettings(5.second, 10.seconds, 0.1))(() =>
          Source.single(request -> 42))
          .via(pool)
          .map(_._1.get)
          .runWith(Sink.head)

      val resp = Await.result(responseFuture, 20.seconds)
      logger.info(" API call status for " +url+" is "+ resp.status + " for event id " + eventId)
      val str = Await.result(Unmarshal(resp.entity).to[String], 20.seconds)

      if (resp.status != StatusCodes.OK || str == null) {
        logger.info("Put request response for  " +url+" is "+ str )
        return null
      }
      str
    }
    catch {
      case ex =>
        logger.error("Exception in  postRequest for event id " + eventId, ex)
        null
    }
  }
  def postRequest(url: String, json: String, eventId: Long): String = {
    try {
      logger.info(" API call - "+url+"  started for event id " + eventId)
      val request = HttpRequest.apply().withMethod(HttpMethods.POST).
        withEntity(HttpEntity(ContentTypes.`application/json`, json)).withUri(url)

      val responseFuture: Future[HttpResponse] =
        RestartSource.onFailuresWithBackoff(RestartSettings(5.second, 10.seconds, 0.1))(() =>
          Source.single(request -> 42))
          .via(pool)
          .map(_._1.get)
          .runWith(Sink.head)

      val resp = Await.result(responseFuture, 20.seconds)
      logger.info(" API call status for url - "+url+" is" + resp.status + " for event id " + eventId)
      val str = Await.result(Unmarshal(resp.entity).to[String], 20.seconds)

      if (resp.status != StatusCodes.OK || str == null) {
        logger.info("EventId- "+eventId+" || Post request response for api - "+url+" is " + str )
        return null
      }
      str
    }
    catch {
      case ex =>
        logger.error("Exception in  postRequest for event id " + eventId, ex)
        null
    }
  }

  def deleteRequest(url: String): Unit = {
    try {
      logger.info(" API call - "+url+"  started")
      val request = HttpRequest.apply().withMethod(HttpMethods.DELETE)
        .withUri(url)

      val responseFuture: Future[HttpResponse] =
        RestartSource.onFailuresWithBackoff(RestartSettings(5.second, 10.seconds, 0.1))(() =>
          Source.single(request -> 42))
          .via(pool)
          .map(_._1.get)
          .runWith(Sink.head)

      val resp = Await.result(responseFuture, 20.seconds)
      logger.info(" DELETE  API call  - "+url+" status " + resp.status)
      Await.result(Unmarshal(resp.entity).to[String], 20.seconds)
    }
    catch {
      case ex => logger.error("Exception in - "+url+"  Delete Request", ex)
        throw ex
    }
  }
}