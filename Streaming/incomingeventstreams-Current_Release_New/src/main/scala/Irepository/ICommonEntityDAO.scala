package Irepository

import entities.EventId


trait ICommonEntityDAO {
  def insertItem[T]( mapperName : String, item : T ): Boolean
  def getEventId[T](mapperName : String) : T
  def getDataWithParam[T](mapperName : String,param: T): T
  def updateItem[T](mapperName : String, item : T )
}
