package code

import java.util.Calendar
import java.util.concurrent.Executors
import core.CommonDBUtil
import util.{APJProcess, APJProcessFuture, DAOProcess, DAOProcessFuture, EMEAProcess, EMEAProcessFuture}
import net.liftweb.json.DefaultFormats
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import spray.json.DefaultJsonProtocol
import akka.util.Timeout
import akka.http.scaladsl.Http
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import scala.concurrent.duration._
import com.typesafe.config.{Config, ConfigFactory}
import Irepository.{ICommonEntityDAO, IInMemoryDataEntityDAO}
import config.{IConnection, MyBatisConnection}
import entities.CommonEntity
import repository.{CommonEntityDAO, InMemoryDataEntityDAO}
import com.google.inject.Inject
import org.slf4j.LoggerFactory.getLogger
import scala.languageFeature.postfixOps
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import code.Orchestrator.execCon
import kafka.KafkaProducer
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


trait JsonProtocol extends DefaultJsonProtocol {
  implicit var orderFormat = jsonFormat16(CommonEntity)
}

object RuleEngine extends JsonProtocol {

  implicit val formats = DefaultFormats
  implicit val postfix = scala.language.postfixOps

  //val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))

  @Inject var sessionFac: IConnection = MyBatisConnection
  @Inject var cmnent: ICommonEntityDAO = new CommonEntityDAO(sessionFac)
  @Inject var inme: IInMemoryDataEntityDAO = new InMemoryDataEntityDAO(sessionFac)

  val cmndbUtil = new CommonDBUtil(cmnent, inme)
  val kafkaProducer = new KafkaProducer()

  val config: Config = ConfigFactory.load()
  val logger: org.slf4j.Logger = getLogger(this.getClass)

  trait IRuleEngine
  case class initiateRuleEngine(incomingMessage: CommonEntity, replyTo: ActorRef[OrderReceived]) extends IRuleEngine
  case class OrderReceived() extends IRuleEngine

  val ruleEngineActor: Behavior[IRuleEngine] =
    Behaviors.setup {
      context =>

        Behaviors.supervise (  Behaviors.receiveMessage[IRuleEngine] {

          case initiateRuleEngine(incomingMessage, replyTo) =>
            try {
              logger.info("Inside initiateRuleEngine" + incomingMessage)
              replyTo ! OrderReceived()
              logger.info(s"Event_id ${incomingMessage.event_ID}")
              logger.info(s"Got order ${incomingMessage.sales_ORDER_ID}")
              logger.info(s"Got BUID ${incomingMessage.buid}")

              if(incomingMessage.region != null && !incomingMessage.region.isEmpty) {
                cmndbUtil.updateStatus(incomingMessage.event_ID, "WIP", "Initiate Process", "", 0)

                if (incomingMessage.isRetry.compareToIgnoreCase("true") == 0) {
                  cmndbUtil.purgeInMemData(incomingMessage.event_ID)
                }
                logger.info("Before Process call for event id " + incomingMessage.event_ID)

                var future : Future[String] = null
                incomingMessage.region.toUpperCase() match {
                  case "APJ" =>
                    future = Future[String] { APJProcessFuture.ProcessAPJOrderFuture(incomingMessage) }(execCon)
                  case "EMEA" =>
                    future = Future[String] { EMEAProcessFuture.ProcessEMEAOrders(incomingMessage) }(execCon)
                  case "DAO" =>
                    future = Future[String] { DAOProcessFuture.ProcessDAOOrders(incomingMessage) }(execCon)
                  case _ =>
                    future = Future[String] {
                      logger.info("region is different from expected for event id " + incomingMessage.event_ID)
                      cmndbUtil.updateStatus(incomingMessage.event_ID, "Failed", "", "region is different from expected for event id " + incomingMessage.event_ID, 0)
                      incomingMessage.event_ID.toString
                    }(execCon)
                }

                future.onComplete{
                  case Success(x) => {
                    logger.info("Completed Process for event id " +incomingMessage.event_ID + "  " + x )
                  }
                  case Failure(e) =>
                    logger.error("Process failed for event id " + incomingMessage.event_ID, e.printStackTrace)
                    kafkaProducer.pushMessagetoRetry(incomingMessage, "Process Failed ")
                } (execCon)
              }
              else {
                logger.info("Region is null for event id" + incomingMessage.event_ID)
                cmndbUtil.updateStatus(incomingMessage.event_ID, "Failed", "", "Region is null for event id " + incomingMessage.event_ID, 0)
              }
            }
            catch{
              case ex =>
                kafkaProducer.pushMessagetoRetry(incomingMessage, "exception in Orchestrator initiate rule engine")
                logger.error("Error in Orchestrtor initiate rule engine for event id  " + incomingMessage.event_ID , ex)
            }
            Behaviors.same
        }).onFailure(SupervisorStrategy.resume)
    }
}

object Orchestrator extends App with JsonProtocol with SprayJsonSupport {

  import RuleEngine._

  implicit val to = Timeout(29 second)

  implicit val backlogEngineActorsystem = ActorSystem(ruleEngineActor, "RetryBacklog")
  val execCon = backlogEngineActorsystem.executionContext

  val engineRoute = path("order" / "process") {
    post {
      entity(as[CommonEntity]) { salesOrder =>
        val validationResponse = backlogEngineActorsystem.ask(ref => initiateRuleEngine(salesOrder, ref)) //fileActor ? initiateRuleEngine(salesOrder,context.self)(to,scheduler)
        complete(validationResponse.map {
          case RuleEngine.OrderReceived() => StatusCodes.OK
          case _ => StatusCodes.BadRequest
        }(execCon))
      }
    }
  }

  logger.info("Start Time : " + Calendar.getInstance.getTime)
  Http().newServerAt( "0.0.0.0", 8082).bind(engineRoute)
  logger.info("Sleeping..." )
}