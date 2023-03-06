package entities

case class Obj_DRGN_ORDER_DETAIL(
                                  buid: Long,
                                  orderNum: String,
                                  commodityCode: Option[String],
                                  ssc: Option[String],
                                  isFgaSku: Option[String],
                                  orderQty: Option[Long],
                                  skuQty: Option[Long],
                                  itemBomQty: Option[Long],
                                  productType: String,
                                  partNum: Option[String],
                                  tieNumber: Long,
                                  itemLineNum: Long,
                                  region: Option[String]
                                )
case class DRGN_ORDER_DETAIL(
                                  buid: Long,
                                  orderNum: String,
                                  commodityCode: String,
                                  ssc: String,
                                  isFgaSku: String,
                                  orderQty: Long,
                                  skuQty: Long,
                                  itemBomQty: Long,
                                  productType: String,
                                  partNum: String,
                                  tieNumber: Long,
                                  itemLineNum: Long,
                                  region: String,
                                  eventId: Long
                                )
