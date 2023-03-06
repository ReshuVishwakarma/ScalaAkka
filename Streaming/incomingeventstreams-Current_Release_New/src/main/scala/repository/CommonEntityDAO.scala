package repository

import Irepository.ICommonEntityDAO
import code.ProcessBacklog.{dslogger, logger}
import config.{IConnection, MyBatisConnection}
import org.apache.ibatis.session.{SqlSession, SqlSessionFactory}
import com.google.inject.Inject
import org.apache.ibatis.exceptions.PersistenceException

class CommonEntityDAO @Inject()(sessionFactory : IConnection) extends ICommonEntityDAO{

  final var sqlSessionFactory:IConnection = _;

  {
    this.sqlSessionFactory = sessionFactory
  }

  // Inserting stream values into UPB_EVENTS for audit and further processing
  @Override
  def insertItem[T] (mapperName : String, item : T ): Boolean = {

    var sessionFactory: SqlSessionFactory = null
    var sqlSession : SqlSession = null

    logger.info("Oracle insertion start...")
    try {
      sessionFactory = sqlSessionFactory.getConnection
      sqlSession = sessionFactory.openSession()
      sqlSession.insert(mapperName, item)
      sqlSession.commit()
      true
    } catch {
      case ex: PersistenceException => {
        logger.error("SQLException while inserting into table " + mapperName, ex)
        if (null != sqlSession) {
          sqlSession.rollback
        }
        throw ex
      }
      case ex => {
        dslogger.error("SQLException while inserting into table " + mapperName, ex, ex.getCause.toString)
        logger.error("SQLException while inserting into table " + mapperName , ex)
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

  @Override
  def getEventId[T](mapperName : String): T ={
    var sessionFactory: SqlSessionFactory = null
    var sqlSession : SqlSession = null
    logger.info("getting event id ... ")
    try {
      sessionFactory = sqlSessionFactory.getConnection
      sqlSession = sessionFactory.openSession()
      sqlSession.selectOne(mapperName)
    }
    catch {
      case ex => {
        dslogger.error("SQLException while getting data "  +  mapperName,ex,ex.getCause.toString)
        logger.error("SQLException while getting data" + mapperName)
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

  @Override
  def getDataWithParam[T](mapperName : String,param: T): T ={
    var sessionFactory: SqlSessionFactory = null
    var sqlSession : SqlSession = null
    logger.info("getting data for parameter passed ... " + param)
    try {
      sessionFactory = sqlSessionFactory.getConnection
      sqlSession = sessionFactory.openSession()
      sqlSession.selectOne(mapperName,param)
    }
    catch {
      case ex => {
        dslogger.error("SQLException while getting data "  +  mapperName,ex,ex.getCause.toString)
        logger.error("SQLException while getting data" + mapperName)
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

  @Override
  def updateItem[T](mapperName : String, item : T ) : Unit = {
    var sessionFactory: SqlSessionFactory = null
    var sqlSession : SqlSession = null

    logger.info("Oracle update start...")
    try {
      sessionFactory = sqlSessionFactory.getConnection
      sqlSession = sessionFactory.openSession()
      sqlSession.update(mapperName, item)
      sqlSession.commit()
    } catch {
      case ex: PersistenceException => {
        logger.error("SQLException while updating table" + mapperName , ex)
        if (null != sqlSession) {
          sqlSession.rollback
        }
        throw ex
      }
      case ex => {
        dslogger.error("SQLException while updating table "  +  mapperName ,ex,ex.getCause.toString)
        logger.error("SQLException while updating table " + mapperName, ex)
        if (null != sqlSession)
          sqlSession.rollback
      }
    } finally {
      if (sessionFactory != null)
        if (sqlSession != null)
          sqlSession.close
      logger.info("connection closed!")
    }
  }
}
