package repository

import Irepository.IInMemoryDataEntityDAO
import code.RuleEngine.logger
import config.IConnection
import com.google.inject.Inject
import org.apache.ibatis.session.{SqlSession, SqlSessionFactory}
import java.util.List

class InMemoryDataEntityDAO @Inject()(sessionFactory : IConnection) extends IInMemoryDataEntityDAO{
  final var sqlSessionFactory:IConnection = _;
  {
    this.sqlSessionFactory = sessionFactory
  }

  @Override
  def getDataList[T](mapperName : String) : List[T] = {

    var sessionFactory: SqlSessionFactory = null
    var sqlSession : SqlSession = null
    logger.info("get Datalist started ")
    try {
      sessionFactory = sqlSessionFactory.getConnection
      sqlSession = sessionFactory.openSession()
      sqlSession.selectList(mapperName)
    }
    catch {
      case ex => {
        //dslogger.error("SQLException while inserting into UPB_EVENTS"  +  ex.printStackTrace(),ex,ex.getCause.toString)
        logger.error("SQLException in get list " + mapperName , ex)
        ex.getMessage
        if (null != sqlSession) {
          sqlSession.rollback
        }
        throw ex
      }
    } finally {
      if (sessionFactory != null)
        if (sqlSession != null)
          sqlSession.close
      logger.info("connection closed!")
    }

  }

  def deleteTableData[T](mapperName: String, lst : T) ={
    var sessionFactory: SqlSessionFactory = null
    var sqlSession : SqlSession = null
    logger.info("delete table data ")
    try {
      sessionFactory = sqlSessionFactory.getConnection
      sqlSession = sessionFactory.openSession()
      sqlSession.delete(mapperName,lst)
      sqlSession.commit()
    }
    catch {
      case ex => {
        //dslogger.error("SQLException while inserting into UPB_EVENTS"  +  ex.printStackTrace(),ex,ex.getCause.toString)
        logger.error("SQLException in delete item " + mapperName, ex)
        ex.getMessage
        if (null != sqlSession) {
          sqlSession.rollback
        }
        throw ex
      }
    } finally {
      if (sessionFactory != null)
        if (sqlSession != null)
          sqlSession.close
      logger.info("connection closed!")
    }

  }

  def getItemList[T](mapperName : String,item: T) : List[T] = {
    var sessionFactory: SqlSessionFactory = null
    var sqlSession : SqlSession = null
    logger.info("getting Item List ... ")
    try {
      sessionFactory = sqlSessionFactory.getConnection
      sqlSession = sessionFactory.openSession()
      sqlSession.selectList(mapperName,item)
    }
    catch {
      case ex => {
        //dslogger.error("SQLException while inserting into UPB_EVENTS"  +  ex.printStackTrace(),ex,ex.getCause.toString)
        logger.error("SQLException in get itemslist " + mapperName , ex)
        ex.getMessage
        if (null != sqlSession) {
          sqlSession.rollback
        }
        throw ex
      }
    } finally {
      if (sessionFactory != null)
        if (sqlSession != null)
          sqlSession.close
      logger.info("connection closed!")
    }
  }

  def UpdateTable[T](mapperName : String,item: T) : Unit = {
    var sessionFactory: SqlSessionFactory = null
    var sqlSession : SqlSession = null
    logger.info("update item started ")
    try {
      sessionFactory = sqlSessionFactory.getConnection
      sqlSession = sessionFactory.openSession()
      sqlSession.update(mapperName,item)
      sqlSession.commit()
    }
    catch {
      case ex => {
        //dslogger.error("SQLException while inserting into UPB_EVENTS"  +  ex.printStackTrace(),ex,ex.getCause.toString)
        logger.error("SQLException in update table " + mapperName ,ex)
        ex.getMessage
        if (null != sqlSession) {
          sqlSession.rollback
        }
        throw ex
      }
    } finally {
      if (sessionFactory != null)
        if (sqlSession != null)
          sqlSession.close
      logger.info("connection closed!")
    }
  }
}
