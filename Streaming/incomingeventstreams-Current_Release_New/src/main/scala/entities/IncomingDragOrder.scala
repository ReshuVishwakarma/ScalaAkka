package entities

case class IncomingDragOrder(
                              table: String,
                              op_type: String,
                              op_ts: String,
                              current_ts: String,
                              pos: String,
                              after: DOrder)

case class DOrder(
                   ORDER_NUM: String,
                   TIE_NUM: Int,
                   PART_NUM: String,
                   BUID: Int,
                   ITEM_LINE_NUM: Int,
                   MODIFIED_DATE: String
                 )