package entities

case class CommonEntity(
                         order_STATUS: String,
                         region: String,
                         event_TYPE: String,
                         event_ID: Long,
                         buid: Int,
                         sales_ORDER_ID: String,
                         status: String,
                         comments: String,
                         parent_EVENT_ID: Long,
                         sys_LAST_MODIFIED_DATE: String,
                         sub_STATUS: String,
                         update_DATE: String,
                         prod_ORDER_ID: String,
                         kafka_TOPIC: String,
                         retry_COUNT:Int,
                         isRetry: String
                       )
case class CommonEntityDB(
                         order_STATUS: String,
                         region: String,
                         event_TYPE: String,
                         event_ID: Long,
                         buid: Int,
                         sales_ORDER_ID: String,
                         status: String,
                         comments: String,
                         parent_EVENT_ID: Long,
                         sys_LAST_MODIFIED_DATE: String,
                         sub_STATUS: String,
                         update_DATE: String,
                         prod_ORDER_ID: String,
                         kafka_TOPIC: String,
                         retry_COUNT:Int,
                         isRetry: String,
                         pod_Name : String
                       )

case class EventId(
                    eventId: Long
                  )

case class StrEntity(
                value : String
                )

case class DomsStatus(
                     status: String,
                     region : String
                     )

case class ParameterEntity(
                            parameterName: String,
                            parameterMethod : String,
                            parameterClass : String
                     )

case class BacklogProcess (
salesOrder : String,
buid : Int,
Url: String,
salesOrderRef : String,
prodNumber : String
)