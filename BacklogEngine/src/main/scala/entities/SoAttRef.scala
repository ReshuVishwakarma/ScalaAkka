package entities

import java.util.Date

case class ObjSoAttRef(
                        soAttributeRef : Long,
                        salesOrderRef : Long,
                        attributeName : String,
                        attributeValue : String,
                        createBy : String,
                        createDate : String,
                        updateBy : String,
                        updateDate : String,
                        srcTransTimestamp : String,
                        updateTimestamp :String,
                        sourceDb : String
                      )

case class SoAttRef(
                     soAttributeRef : Long,
                     salesOrderRef : Long,
                     attributeName : String,
                     attributeValue : String,
                     createBy : String,
                     createDate : Date,
                     updateBy : String,
                     updateDate : Date,
                     srcTransTimestamp : Date,
                     updateTimestamp :Date,
                     //UPB_CREATE_DATE
                     sourceDb: String,
                     eventId: Long

                   )

