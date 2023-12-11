/*
 * Part of NDLA myndla-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.integration

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import no.ndla.myndlaapi.Props

trait DataSource {
  this: Props =>

  import props._
  object DataSource {

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

    def getLpDs: HikariDataSource = {
      val dataSourceConfig = new HikariConfig()
      dataSourceConfig.setUsername(LpMetaUserName)
      dataSourceConfig.setPassword(LpMetaPassword)
      dataSourceConfig.setJdbcUrl(
        s"jdbc:postgresql://$LpMetaServer:$LpMetaPort/$LpMetaResource?ApplicationName=$ApplicationName"
      )
      dataSourceConfig.setDriverClassName("org.postgresql.Driver")
      dataSourceConfig.setSchema(LpMetaSchema)
      dataSourceConfig.setMaximumPoolSize(LpMetaMaxConnections)
      new HikariDataSource(dataSourceConfig)
    }
  }

}
