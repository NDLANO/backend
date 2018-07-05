/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import no.ndla.audioapi.auth.{Role, User}
import no.ndla.audioapi.controller.HealthController
import no.ndla.audioapi.controller.{AudioController, InternController}
import no.ndla.audioapi.integration._
import no.ndla.audioapi.repository.AudioRepository
import no.ndla.audioapi.service.search.{IndexService, _}
import no.ndla.audioapi.service._
import no.ndla.network.NdlaClient
import org.postgresql.ds.PGPoolingDataSource
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

object ComponentRegistry
    extends DataSource
    with AudioRepository
    with NdlaClient
    with MigrationApiClient
    with ImportService
    with TagsService
    with AmazonClient
    with ReadService
    with WriteService
    with DraftApiClient
    with ValidationService
    with ConverterService
    with AudioStorageService
    with InternController
    with HealthController
    with AudioController
    with SearchService
    with Elastic4sClient
    with IndexService
    with SearchConverterService
    with SearchIndexService
    with User
    with Role
    with Clock {
  def connectToDatabase(): Unit = ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))
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

  connectToDatabase()

  val amazonClient = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_CENTRAL_1).build()
  lazy val storageName = AudioApiProperties.StorageName

  lazy val audioRepository = new AudioRepository
  lazy val audioStorage = new AudioStorage

  lazy val ndlaClient = new NdlaClient
  lazy val migrationApiClient = new MigrationApiClient
  lazy val draftApiClient = new DraftApiClient

  lazy val importService = new ImportService
  lazy val readService = new ReadService
  lazy val writeService = new WriteService
  lazy val validationService = new ValidationService
  lazy val converterService = new ConverterService
  lazy val tagsService = new TagsService

  lazy val internController = new InternController
  lazy val resourcesApp = new ResourcesApp
  lazy val audioApiController = new AudioController
  lazy val healthController = new HealthController

  lazy val e4sClient = Elastic4sClientFactory.getClient()
  lazy val indexService = new IndexService
  lazy val searchConverterService = new SearchConverterService
  lazy val searchIndexService = new SearchIndexService
  lazy val searchService = new SearchService

  lazy val authRole = new AuthRole
  lazy val authUser = new AuthUser
  lazy val clock = new SystemClock

}
