/*
package apiCode
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import spray.json._

object SO_Domain{
  case class Sales_Order(orderNo:String,totalLines:Int,orderDate:String)
  case class Order_Shipment(salesOrder: Sales_Order,shipMethod:String,shipQty:Int)
  case object OrderShipAllowed
  case object OrderShipNotAllowed
}

trait SOJsonProtocol extends DefaultJsonProtocol{
  implicit val orderFormat = jsonFormat3(SO_Domain.Sales_Order)
  implicit val shipmentFormat = jsonFormat3(SO_Domain.Order_Shipment)
}

object SalesOrderClient extends App with SOJsonProtocol {
  import SO_Domain._

  implicit val system = ActorSystem("SOValidatorClient")
  implicit val materializer = ActorMaterializer()
  val salesOrders = List(Sales_Order("SO1",13,"24/03/2021"),
    Sales_Order("SO2",15,"25/03/2021"),
    Sales_Order("SO3",1,"25/03/2021"))

  val shipmentRecords = salesOrders.map(so => Order_Shipment(so,"Flight",so.totalLines*100))
  val soHttpReq = shipmentRecords.map(shipRec =>
        HttpRequest(
          HttpMethods.POST,
          uri = Uri("/so/validate"),
          entity = HttpEntity(ContentTypes.`application/json`,
          shipRec.toJson.prettyPrint)
        ))

  Source(soHttpReq).
    via(Http().outgoingConnection("mtrcbacklogengine-service",8080)).
    to(Sink.foreach[HttpResponse](println)).run()
}
*/
