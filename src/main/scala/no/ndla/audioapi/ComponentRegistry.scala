/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import no.ndla.audioapi.integration.DataSourceComponent
import org.postgresql.ds.PGPoolingDataSource

object ComponentRegistry
  extends DataSourceComponent {
  lazy val dataSource = new PGPoolingDataSource()
  dataSource.setUser(AudioApiProperties.get("META_USER_NAME"))
  dataSource.setPassword(AudioApiProperties.get("META_PASSWORD"))
  dataSource.setDatabaseName(AudioApiProperties.get("META_RESOURCE"))
  dataSource.setServerName(AudioApiProperties.get("META_SERVER"))
  dataSource.setPortNumber(AudioApiProperties.getInt("META_PORT"))
  dataSource.setInitialConnections(AudioApiProperties.getInt("META_INITIAL_CONNECTIONS"))
  dataSource.setMaxConnections(AudioApiProperties.getInt("META_MAX_CONNECTIONS"))
  dataSource.setCurrentSchema(AudioApiProperties.get("META_SCHEMA"))
}
