/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import no.ndla.audioapi.controller.{AudioApiController, InternController}
import no.ndla.audioapi.integration.{DataSourceComponent, MigrationApiClient}
import no.ndla.audioapi.repository.AudioRepositoryComponent
import no.ndla.audioapi.service.{AudioStorageService, ImportServiceComponent}
import no.ndla.network.NdlaClient
import org.postgresql.ds.PGPoolingDataSource
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

object ComponentRegistry
  extends DataSourceComponent
  with AudioRepositoryComponent
  with NdlaClient
  with MigrationApiClient
  with ImportServiceComponent
  with AudioStorageService
  with InternController
  with AudioApiController {

  implicit val swagger = new AudioSwagger

  lazy val dataSource = new PGPoolingDataSource()
  dataSource.setUser(AudioApiProperties.MetaUserName)
  dataSource.setPassword(AudioApiProperties.MetaPassword)
  dataSource.setDatabaseName(AudioApiProperties.MetaResource)
  dataSource.setServerName(AudioApiProperties.MetaServer)
  dataSource.setPortNumber(AudioApiProperties.MetaPort)
  dataSource.setInitialConnections(AudioApiProperties.MetaInitialConnections)
  dataSource.setMaxConnections(AudioApiProperties.MetaMaxConnections)
  dataSource.setCurrentSchema(AudioApiProperties.MetaSchema)

  ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

  lazy val audioRepository = new AudioRepository
  lazy val importService = new ImportService
  lazy val audioStorage = new AudioStorage
  lazy val ndlaClient = new NdlaClient
  lazy val migrationApiClient = new MigrationApiClient

  lazy val internController = new InternController
  lazy val resourcesApp = new ResourcesApp
  lazy val audioApiController = new AudioApiController
}
