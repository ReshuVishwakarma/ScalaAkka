package entities

case class ObjPart(
                    lineSkuRef: Option[String],
                    partSeq: Option[Long],
                    pLineSkuRef: Option[Long],
                    pPartSeq: Option[Long],
                    partNum: Option[String],
                    issueCode: Option[Long],
                    boxCode: Option[String],
                    consumptionFacility: Option[String],
                    qtyPerSku: Option[Long],
                    partType: Option[String],
                    region: Option[String],
                    createDate: Option[String],
                    dmsFlag: Option[String],
                    itemCategory: Option[String],
                    qty: Option[Long],
                    isSystem : Option[String]
                  )

case class Part(
                 lineSkuRef: String,
                 partSeq: Long,
                 pLineSkuRef: Long,
                 pPartSeq: Long,
                 partNum: String,
                 issueCode: Long,
                 boxCode: String,
                 consumptionFacility: String,
                 qtyPerSku: Long,
                 partType: String,
                 region: String,
                 createDate: String,
                 dmsFlag: String,
                 itemCategory: String,
                 qty: Long,
                 eventId: Long,
                 isSystem: String
               )
