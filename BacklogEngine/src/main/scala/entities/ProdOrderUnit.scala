package entities

import java.util.Date

case class ObjProdOrderUnit(
                             soLineRef: Long,
                             unitSeq: String,
                             buid: Long,
                             svcTag: String,
                             region: String,
                             statusCode: String,
                             statusDate: String,
                             createDate: String,
                             createBy: String,
                             updateDate: String,
                             updateBy: String,
                             srcTransTimeStamp: String,
                             updateTimeStamp: String
                           )

case class ProdOrderUnit(
                             soLineRef: Long,
                             unitSeq: String,
                             buid: Long,
                             svcTag: String,
                             region: String,
                             statusCode: String,
                             statusDate: Date,
                             createDate: Date,
                             createBy: String,
                             updateDate: Date,
                             updateBy: String,
                             srcTransTimeStamp: String,
                             updateTimeStamp: String,
                             eventId: Long
                           )

