/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.integration

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import no.ndla.conceptapi.ConceptApiProperties._

trait DataSource {
  val dataSource: HikariDataSource

}

object DataSource {

  def getHikariDataSource: HikariDataSource = {
    val dataSourceConfig = new HikariConfig()
    dataSourceConfig.setUsername(MetaUserName)
    dataSourceConfig.setPassword(MetaPassword)
    dataSourceConfig.setJdbcUrl(s"jdbc:postgresql://$MetaServer:$MetaPort/$MetaResource")
    dataSourceConfig.setSchema(MetaSchema)
    dataSourceConfig.setMaximumPoolSize(MetaMaxConnections)
    new HikariDataSource(dataSourceConfig)
  }
}
