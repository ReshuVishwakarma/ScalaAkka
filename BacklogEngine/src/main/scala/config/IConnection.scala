package config

import org.apache.ibatis.session.SqlSessionFactory

trait IConnection {
  def getConnection: SqlSessionFactory
}
