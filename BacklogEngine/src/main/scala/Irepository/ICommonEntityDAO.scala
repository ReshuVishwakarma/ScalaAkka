package Irepository

trait ICommonEntityDAO {
  def insertItem[T]( mapperName : String, item : T ): Boolean
  def insertItem[T]( mapperName : String, item : List[T] ): Boolean
  def getItem[T](mapperName : String) : T
  def getItem[T](mapperName : String,item: T) : T

}
