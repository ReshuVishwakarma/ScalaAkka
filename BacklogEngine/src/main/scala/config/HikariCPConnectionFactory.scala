package config

import com.zaxxer.hikari.HikariDataSource
import org.apache.ibatis.datasource.unpooled.UnpooledDataSourceFactory


class HikariCPConnectionFactory extends UnpooledDataSourceFactory {
  this.dataSource = new HikariDataSource()
}


