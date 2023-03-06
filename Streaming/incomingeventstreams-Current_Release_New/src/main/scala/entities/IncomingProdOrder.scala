package entities

case class IncomingProdOrder(
                              table: String,
                              op_type: String,
                              op_ts: String,
                              current_ts: String,
                              pos: String,
                              after: POrder)

case class POrder(
                   SALES_ORDER_ID: String,
                   BUID: Int,
                   PROD_ORDER_NUM: String,
                   STATUS_CODE: String,
                   STATUS_DATE: String,
                   CREATE_DATE: String,
                   CREATE_BY: String,
                   REGION: String
                 )