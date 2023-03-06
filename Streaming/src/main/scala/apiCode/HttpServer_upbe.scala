/*
package apiCode

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.IncomingConnection
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object HttpServer_upbe extends App{
  implicit val system = ActorSystem("LowLevel_ServerAPI")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  val serverSource = Http().bind("localhost",8001)
  val connSink = Sink.foreach[IncomingConnection]{
                  conn => println(s"Incoming connection request from ${conn.remoteAddress}")}

  val serverBindingFuture = serverSource.to(connSink).run()
  serverBindingFuture.onComplete{
    case Success(binding) => println("Server Binding successful")
                      binding.terminate(5 seconds)
    case Failure(e) => println(s"Problem in server binding with $e")
  }

  /*Request handler */
  //Synchronus

  val reqHandler: HttpRequest => HttpResponse = {
    case HttpRequest(HttpMethods.GET, uri, headers, entity, protocol) =>
          HttpResponse(
            StatusCodes.OK,
            entity = HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            """
              |<html>
              | <body> Hello from akka http </body>
              |</html>""".stripMargin
            )
          )
    case req : HttpRequest =>
          req.discardEntityBytes()
          HttpResponse(
            StatusCodes.NotFound,
            entity = HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            """
                |<html>
                |<body> Not found!! </body>
                |</html>""".stripMargin
            )
          )
    }


  val httpConnSyncHandler = Sink.foreach[IncomingConnection]{
    conn => conn.handleWithSyncHandler(reqHandler)
  }

  //Http().bindAndHandleSync(reqHandler,"localhost",8340) //short-hand
  //Http().bind("localhost",8338).runWith(httpConnSyncHandler)

  //async
  val asyncReqHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.GET, Uri.Path("/home"), headers, entity, protocol) =>
      Future(HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body> Hello from akka http async</body>
            |</html>""".stripMargin
        )
      ))
    case req : HttpRequest =>
      req.discardEntityBytes()
      Future(HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |<body> Not found!! </body>
            |</html>""".stripMargin
        )
      ))
  }

  val httpConnAsyncHandler = Sink.foreach[IncomingConnection]{
    conn => conn.handleWithAsyncHandler(asyncReqHandler)
  }

  Http().bind("localhost",8082).runWith(httpConnAsyncHandler)

}
*/
