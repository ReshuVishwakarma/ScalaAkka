package entities

case class Obj_DRGN_FULFILLMENT(
                                 buid: Long,
                                 orderNum: String,
                                 status: Option[String],
                                 updateDate: String,
                                 partNum: Option[String],
                                 tieNum: Long,
                                 itemLineNum: Long,
                                 region: Option[String],
                                 modNum: Option[String],
                                 orderQty: Option[Long]
                               )

case class DRGN_FULFILLMENT(
                                 buid: Long,
                                 orderNum: String,
                                 status: String,
                                 updateDate: String,
                                 partNum: String,
                                 tieNum: Long,
                                 itemLineNum: Long,
                                 region: String,
                                 modNum: String,
                                 orderQty: Long,
                                 eventId: Long
                               )