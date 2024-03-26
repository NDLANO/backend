/*
 * Part of NDLA myndla-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.integration

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import no.ndla.myndlaapi.Props
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

trait DataSource {
  this: Props =>

  val dataSource: HikariDataSource

  import props._
  object DataSource {

    def connectToDatabase(): Unit = {
      ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))
    }

    def getHikariDataSource: HikariDataSource = {
      val dataSourceConfig = new HikariConfig()
      dataSourceConfig.setUsername(MetaUserName)
      dataSourceConfig.setPassword(MetaPassword)
      dataSourceConfig.setJdbcUrl(
        s"jdbc:postgresql://$MetaServer:$MetaPort/$MetaResource?ApplicationName=$ApplicationName"
      )
      dataSourceConfig.setDriverClassName("org.postgresql.Driver")
      dataSourceConfig.setSchema(MetaSchema)
      dataSourceConfig.setMaximumPoolSize(MetaMaxConnections)
      new HikariDataSource(dataSourceConfig)
    }
  }

}
