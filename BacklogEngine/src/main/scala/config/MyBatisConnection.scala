package config

import org.apache.ibatis.session.SqlSessionFactory
import org.apache.ibatis.session.SqlSessionFactoryBuilder
import java.io.FileInputStream
import java.io.InputStream
import code.RuleEngine.logger
import constant.PropertiesLoader.myBatisConfiguration


object MyBatisConnection extends IConnection{

  var sqlSessionFactory : SqlSessionFactory = _

  {
    try {
      val inputStream: InputStream = new FileInputStream(myBatisConfiguration);
      //val inputStream: InputStream = getClass().getClassLoader().getResourceAsStream("databaseConfig/DBconfiguration.xml");
      sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream)
    } catch {
      case e =>
        logger.error("Error while instantiating Mybatis " + e.getCause)
        throw new RuntimeException("Fatal Error.  Cause: " + e, e);
    }
  }


  @Override
  def  getConnection : SqlSessionFactory = {
    if(sqlSessionFactory == null){
      createConnection
    }
    sqlSessionFactory
  }

  def createConnection{

    val dbConfigFile = System.getenv("DB_CONFIG")
    var inputStream : InputStream = null
    try {
      if(dbConfigFile != null){
        inputStream= new FileInputStream(dbConfigFile)
      }
      else {
        logger.info("Working Directory = " + System.getProperty("user.dir"))
        inputStream = new FileInputStream(System.getProperty("user.dir") + "/certs/databaseConfig/DBConfiguration.xml");
      }
      sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

    } catch {
      case e =>
        logger.error("Error while creating connection " + e.getCause)
        throw new RuntimeException("Fatal Error.  Cause: " + e, e);
    }
  }
}




