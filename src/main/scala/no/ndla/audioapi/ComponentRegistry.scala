/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import no.ndla.audioapi.controller.InternController
import no.ndla.audioapi.integration.DataSourceComponent
import no.ndla.audioapi.repository.AudioRepositoryComponent
import org.postgresql.ds.PGPoolingDataSource
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

object ComponentRegistry
  extends DataSourceComponent
  with AudioRepositoryComponent
  with InternController {

  lazy val dataSource = new PGPoolingDataSource()
  dataSource.setUser(AudioApiProperties.MetaUserName)
  dataSource.setPassword(AudioApiProperties.MetaPassword)
  dataSource.setDatabaseName(AudioApiProperties.MetaPassword)
  dataSource.setServerName(AudioApiProperties.MetaServer)
  dataSource.setPortNumber(AudioApiProperties.MetaPort)
  dataSource.setInitialConnections(AudioApiProperties.MetaInitialConnections)
  dataSource.setMaxConnections(AudioApiProperties.MetaMaxConnections)
  dataSource.setCurrentSchema(AudioApiProperties.MetaSchema)

  ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

  lazy val audioRepository = new AudioRepository
  lazy val internController = new InternController
}
