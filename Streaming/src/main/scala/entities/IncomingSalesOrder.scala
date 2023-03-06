package entities

case class IncomingSalesOrder(
                               table: String,
                               op_type: String,
                               op_ts: String,
                               current_ts: String,
                               pos: String,
                               after: SOrder)

case class SOrder(
                   SALES_ORDER_ID: String,
                   BUID: Int,
                   DOMS_STATUS: String,
                   STATUS_DATE: String,
                   CREATE_BY: String,
                   CREATE_DATE: String,
                   SYSTEM_QTY: Option[String],
                   REGION: String
                 )
