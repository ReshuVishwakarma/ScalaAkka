package entities

import java.util.Date

case class ObjProdOrder(
                         prodOrderNum: String,
                         salesOrderRef: String,
                         ccn: String,
                         channelCode: String,
                         hasFga: String,
                         buildType: String,
                         facility: String,
                         statusCode: String,
                         heldCode: String,
                         primaryFc: String,
                         isRetail: String,
                         retailerName: String,
                         shipToFacility: String,
                         orderPriority: Long,
                         updateDate: String,
                         subChannel: String,
                         boxingFacility: Option[String],
                         kittingFacility: Option[String]
                       )

case class ProdOrder(
                         prodOrderNum: String,
                         salesOrderRef: String,
                         ccn: String,
                         channelCode: String,
                         hasFga: String,
                         buildType: String,
                         facility: String,
                         statusCode: String,
                         heldCode: String,
                         primaryFc: String,
                         isRetail: String,
                         retailerName: String,
                         shipToFacility: String,
                         orderPriority: Long,
                         updateDate: Date,
                         subChannel: String,
                         eventId: Long,
                         boxingFacility: String,
                         kittingFacility: String
                       )
