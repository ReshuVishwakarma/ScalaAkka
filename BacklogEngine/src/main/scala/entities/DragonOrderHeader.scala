package entities

import java.util.Date

case class Obj_DRGN_ORDER_HEADER(
                                  prodOrderNum: String,
                                  orderNum: String,
                                  buid: Long,
                                  currentStatus: String,
                                  ccnCode: String,
                                  fulfillmentChannel: String,
                                  customerNum: String,
                                  orderDate: String,
                                  centralLocation: String,
                                  countryCode: String,
                                  mergeOrderFlag: String,
                                  status: Option[String],
                                  region: Option[String],
                                  ibu : Option[String]
                                )


case class DRGN_ORDER_HEADER(
                                  prodOrderNum: String,
                                  orderNum: String,
                                  buid: Long,
                                  currentStatus: String,
                                  ccnCode: String,
                                  fulfillmentChannel: String,
                                  customerNum: String,
                                  orderDate: Date,
                                  centralLocation: String,
                                  countryCode: String,
                                  mergeOrderFlag: String,
                                  status: String,
                                  region: String,
                                  eventId: Long,
                                  ibu : String
                                )

