package entities

case class ASNEntity(
                      EntityValue : String,
                      EventID : String,
                      EventName : String,
                      State:String,
                      Channel:String,
                      DataSource:String,
                      FinalASN:String,
                      //TraceID:String,
                      Region:String,
                      ShipFrom:String,
                      ShipTo:String,
                      VendorName:String
                      //eve_ID: Long


                    )

case class jsonent(
                    EntityType : String,
                    EntityValue : Array[String],
                    //EntityValue : String,
                    EventID : String,
                    EventTimestamp: String,
                    Version : String,
                    Attributes :  List[Attribute]
                  )

case class Attribute(
                      Name : String,
                      Value: String
                    )
