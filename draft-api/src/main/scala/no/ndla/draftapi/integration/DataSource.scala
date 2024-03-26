/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.integration

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import no.ndla.draftapi.Props
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

trait DataSource {
  this: Props =>
  val dataSource: HikariDataSource
  import props._
  object DataSource {

    def getHikariDataSource: HikariDataSource = {
      val dataSourceConfig = new HikariConfig()
      dataSourceConfig.setUsername(MetaUserName)
      dataSourceConfig.setPassword(MetaPassword)
      dataSourceConfig.setJdbcUrl(s"jdbc:postgresql://$MetaServer:$MetaPort/$MetaResource")
      dataSourceConfig.setDriverClassName("org.postgresql.Driver")
      dataSourceConfig.setSchema(MetaSchema)
      dataSourceConfig.setMaximumPoolSize(MetaMaxConnections)
      new HikariDataSource(dataSourceConfig)
    }

    def connectToDatabase(): Unit = ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))
  }

}
