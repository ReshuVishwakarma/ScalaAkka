package entities

case class ObjSalesOrderLine(
                              soLineRef: String,
                              salesOrderRef: String,
                              prodOrderNum: String,
                              lineNum: String,
                              lineQty: Long,
                              tieNum: String,
                              lob: String,
                              baseType: String,
                              workCenter: String,
                              ssc: String,
                              region: String,
                              mfgMethod: String,
                              mfgLob: String
                            )

case class SalesOrderLine(
                              soLineRef: String,
                              salesOrderRef: String,
                              prodOrderNum: String,
                              lineNum: String,
                              lineQty: Long,
                              tieNum: String,
                              lob: String,
                              baseType: String,
                              workCenter: String,
                              ssc: String,
                              region: String,
                              mfgMethod: String,
                              mfgLob: String,
                              eventId: Long
                            )

