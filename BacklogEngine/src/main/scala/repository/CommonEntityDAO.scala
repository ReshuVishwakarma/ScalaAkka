package repository

import Irepository.ICommonEntityDAO
import code.RuleEngine.logger
import config.IConnection
import org.apache.ibatis.session.{SqlSession, SqlSessionFactory}
import com.google.inject.Inject

class CommonEntityDAO @Inject()(sessionFactory : IConnection) extends ICommonEntityDAO{

  final var sqlSessionFactory:IConnection = _;
  {
    this.sqlSessionFactory = sessionFactory
  }

  @Override
  def insertItem[T]( mapperName : String, item : List[T] ): Boolean = {

    var sessionFactory: SqlSessionFactory = null
    var sqlSession : SqlSession = null

    logger.info("Oracle insertion list start...")
    try {
      sessionFactory = sqlSessionFactory.getConnection
      sqlSession = sessionFactory.openSession()
      sqlSession.insert(mapperName, item)
      sqlSession.commit()
      true
    } catch {
      case ex => {
        //dslogger.error("SQLException while inserting into UPB_EVENTS"  +  ex.printStackTrace(),ex,ex.getCause.toString)
        logger.error("SQLException while inserting into " + mapperName, ex)
        ex.getMessage
        if (null != sqlSession)
          sqlSession.rollback
        throw ex
      }
    } finally {
      if (sessionFactory != null)
        if (sqlSession != null)
          sqlSession.close
      logger.info("connection closed!")
    }
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
      case ex => {
        //dslogger.error("SQLException while inserting into UPB_EVENTS"  +  ex.printStackTrace(),ex,ex.getCause.toString)
        logger.error("SQLException while inserting into " + mapperName, ex)
        if (null != sqlSession)
          sqlSession.rollback
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
  def getItem[T](mapperName : String): T ={
    var sessionFactory: SqlSessionFactory = null
    var sqlSession : SqlSession = null
    logger.info("get item with no parameter start ... ")
    try {
      sessionFactory = sqlSessionFactory.getConnection
      sqlSession = sessionFactory.openSession()
      sqlSession.selectOne(mapperName)
    }
    catch {
      case ex => {
        //dslogger.error("SQLException while inserting into UPB_EVENTS"  +  ex.printStackTrace(),ex,ex.getCause.toString)
        logger.error("SQLException in get item no parameter " + mapperName, ex)
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
  def getItem[T](mapperName : String,item: T) : T={
    var sessionFactory: SqlSessionFactory = null
    var sqlSession : SqlSession = null
    logger.info("get item with parameters ... ")
    try {
      sessionFactory = sqlSessionFactory.getConnection
      sqlSession = sessionFactory.openSession()
      sqlSession.selectOne(mapperName,item)
    }
    catch {
      case ex => {
        //dslogger.error("SQLException while inserting into UPB_EVENTS"  +  ex.printStackTrace(),ex,ex.getCause.toString)
        logger.error("SQLException in get item with parameter " + mapperName, ex)
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
