package entities

case class ObjSalesOrder(
                          salesOrderId: String,
                          salesOrderRef: String,
                          region: String,
                          sourceSystemStatusCode: String,
                          buId: Long,
                          orderDate: String,
                          ipDate: String,
                          shipByDate: String,
                          customerNum: String,
                          companyNum: String,
                          itemQty: Long,
                          systemQty: Long,
                          state: String,
                          country: String,
                          statusCode: String,
                          shipCode: String,
                          mustArriveByDate: String,
                          fulfillmentStatusCode : String
                        )

case class SalesOrder(
                          salesOrderId: String,
                          salesOrderRef: String,
                          region: String,
                          sourceSystemStatusCode: String,
                          buId: Long,
                          orderDate: String,
                          ipDate: String,
                          shipByDate: String,
                          customerNum: String,
                          companyNum: String,
                          itemQty: Long,
                          systemQty: Long,
                          state: String,
                          country: String,
                          statusCode: String,
                          shipCode: String,
                          mustArriveByDate: String,
                          fulfillmentStatusCode : String,
                          eventId: Long
                        )

