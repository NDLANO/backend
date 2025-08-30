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

class DataSource(hikariConfig: HikariConfig) extends HikariDataSource(hikariConfig) with StrictLogging {
  def connectToDatabase(): Unit = {
    logger.info(s"Connecting to database: ${this.getJdbcUrl}")
    ConnectionPool.singleton(new DataSourceConnectionPool(this))
  }
}

object DataSource extends StrictLogging {
  def getDataSource(using props: DatabaseProps): DataSource = {
    val dataSourceConfig = new HikariConfig()
    dataSourceConfig.setUsername(props.MetaUserName)
    dataSourceConfig.setPassword(props.MetaPassword)
    dataSourceConfig.setJdbcUrl(s"jdbc:postgresql://${props.MetaServer}:${props.MetaPort}/${props.MetaResource}")
    dataSourceConfig.setDriverClassName("org.postgresql.Driver")
    dataSourceConfig.setSchema(props.MetaSchema)
    dataSourceConfig.setMaximumPoolSize(props.MetaMaxConnections)
    new DataSource(dataSourceConfig)
  }
}
