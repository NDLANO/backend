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
import no.ndla.audioapi.controller.HealthController
import no.ndla.audioapi.controller.{AudioApiController, InternController}
import no.ndla.audioapi.integration._
import no.ndla.audioapi.repository.AudioRepository
import no.ndla.audioapi.service.search.{ElasticIndexService, _}
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
  with ConverterService
  with AudioStorageService
  with InternController
  with HealthController
  with AudioApiController
  with SearchService
  with ElasticClient
  with ElasticIndexService
  with SearchConverterService
  with SearchIndexService
{
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

  val amazonClient = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_CENTRAL_1).build()
  lazy val storageName = AudioApiProperties.StorageName

  lazy val audioRepository = new AudioRepository
  lazy val audioStorage = new AudioStorage

  lazy val ndlaClient = new NdlaClient
  lazy val migrationApiClient = new MigrationApiClient

  lazy val importService = new ImportService
  lazy val readService = new ReadService
  lazy val writeService = new WriteService
  lazy val converterService = new ConverterService
  lazy val tagsService = new TagsService

  lazy val internController = new InternController
  lazy val resourcesApp = new ResourcesApp
  lazy val audioApiController = new AudioApiController
  lazy val healthController = new HealthController

  lazy val jestClient = JestClientFactory.getClient()
  lazy val elasticIndexService = new ElasticIndexService
  lazy val searchConverterService = new SearchConverterService
  lazy val searchIndexService = new SearchIndexService
  lazy val searchService = new SearchService
}
