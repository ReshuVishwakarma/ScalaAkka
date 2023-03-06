package entities

case class ObjLSKU(
                    lineSkuRef: String,
                    soLineRef: String,
                    skuHdrRef: String,
                    skuQty: Long,
                    skuNum: String,
                    isFgaSku: String,
                    modQty: Option[Long],
                    baseFlag: Option[String],
                    fgaProxyItem: Option[String],
                    mfgPartNum: Option[String],
                    classCode: Option[String],
                    subClass: Option[String],
                    buid: Long,
                    salesOrderId: String,
                    region: String
                  )

case class LSKU(
                    lineSkuRef: String,
                    soLineRef: String,
                    skuHdrRef: String,
                    skuQty: Long,
                    skuNum: String,
                    isFgaSku: String,
                    modQty: Long,
                    baseFlag: String,
                    fgaProxyItem: String,
                    mfgPartNum: String,
                    classCode: String,
                    subClass: String,
                    buid: Long,
                    salesOrderId: String,
                    region: String,
                    eventId: Long
                  )
