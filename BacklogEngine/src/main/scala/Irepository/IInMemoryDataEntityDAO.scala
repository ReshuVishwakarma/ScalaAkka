package Irepository

import java.util.List
trait IInMemoryDataEntityDAO {
  def getDataList[T](mapperName : String) : List[T]
  def deleteTableData[T](mapperName: String, lst : T)
  def getItemList[T](mapperName : String,item: T) : List[T]
  def UpdateTable[T](mapperName : String,item: T)
}
