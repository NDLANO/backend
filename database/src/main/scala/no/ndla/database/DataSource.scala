/*
 * Part of NDLA database
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.database

import com.typesafe.scalalogging.StrictLogging
import com.zaxxer.hikari.metrics.prometheus.PrometheusHistogramMetricsTrackerFactory
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.prometheus.client.CollectorRegistry
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

class DataSource(hikariConfig: HikariConfig)(using props: DatabaseProps)
    extends HikariDataSource(hikariConfig)
    with StrictLogging {
  def connectToDatabase(): Unit = {
    logger.info(s"Connecting to database: ${this.getJdbcUrl}")
    val connectionPool = new DataSourceConnectionPool(this)
    ConnectionPool.add(props.ApplicationName, connectionPool)
    ConnectionPool.singleton(connectionPool)
    SqlMetrics.install()
  }
}

object DataSource extends StrictLogging {
  def getDataSource(using props: DatabaseProps): DataSource = {
    val dataSourceConfig = new HikariConfig()
    dataSourceConfig.setUsername(props.MetaUserName)
    dataSourceConfig.setPassword(props.MetaPassword)
    dataSourceConfig.setJdbcUrl(s"jdbc:postgresql://${props.MetaServer}:${props.MetaPort}/${props.MetaResource}")
    dataSourceConfig.setDriverClassName("org.postgresql.Driver")
    dataSourceConfig.addDataSourceProperty("socketTimeout", "1800") // 30 minutes
    dataSourceConfig.addDataSourceProperty("loginTimeout", "10")
    dataSourceConfig.addDataSourceProperty("tcpKeepAlive", "true")
    dataSourceConfig.setSchema(props.MetaSchema)
    dataSourceConfig.setMaximumPoolSize(props.MetaMaxConnections)
    dataSourceConfig.setMetricsTrackerFactory(
      new PrometheusHistogramMetricsTrackerFactory(CollectorRegistry.defaultRegistry)
    )
    new DataSource(dataSourceConfig)
  }
}
