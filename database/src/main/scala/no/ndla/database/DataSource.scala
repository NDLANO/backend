/*
 * Part of NDLA database
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.database

import com.typesafe.scalalogging.StrictLogging
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

trait DataSource {
  this: HasDatabaseProps =>

  lazy val dataSource: HikariDataSource

  object DataSource extends StrictLogging {
    def getHikariDataSource: HikariDataSource = {
      val dataSourceConfig = new HikariConfig()
      dataSourceConfig.setUsername(props.MetaUserName)
      dataSourceConfig.setPassword(props.MetaPassword)
      dataSourceConfig.setJdbcUrl(s"jdbc:postgresql://${props.MetaServer}:${props.MetaPort}/${props.MetaResource}")
      dataSourceConfig.setDriverClassName("org.postgresql.Driver")
      dataSourceConfig.setSchema(props.MetaSchema)
      dataSourceConfig.setMaximumPoolSize(props.MetaMaxConnections)
      new HikariDataSource(dataSourceConfig)
    }

    def connectToDatabase(): Unit = {
      logger.info(s"Connecting to database: ${dataSource.getJdbcUrl}")
      ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))
    }
  }
}
