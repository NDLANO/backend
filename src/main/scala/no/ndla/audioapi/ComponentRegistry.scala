/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.s3.AmazonS3Client
import no.ndla.audioapi.controller.HealthController
import no.ndla.audioapi.controller.{AudioApiController, InternController}
import no.ndla.audioapi.integration._
import no.ndla.audioapi.repository.AudioRepositoryComponent
import no.ndla.audioapi.service.search.{ElasticContentIndexComponent, SearchConverterService, SearchIndexServiceComponent, SearchService}
import no.ndla.audioapi.service.{AudioStorageService, ConverterService, ImportServiceComponent, ReadServiceComponent}
import no.ndla.network.NdlaClient
import org.postgresql.ds.PGPoolingDataSource
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

object ComponentRegistry
  extends DataSourceComponent
  with AudioRepositoryComponent
  with NdlaClient
  with MigrationApiClient
  with MappingApiClient
  with ImportServiceComponent
  with AmazonClientComponent
  with ReadServiceComponent
  with ConverterService
  with AudioStorageService
  with InternController
  with HealthController
  with AudioApiController
  with SearchService
  with ElasticClientComponent
  with ElasticContentIndexComponent
  with SearchConverterService
  with SearchIndexServiceComponent
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

  val amazonClient = new AmazonS3Client(new BasicAWSCredentials(AudioApiProperties.StorageAccessKey, AudioApiProperties.StorageSecretKey))
  amazonClient.setRegion(Region.getRegion(Regions.EU_CENTRAL_1))
  lazy val storageName = AudioApiProperties.StorageName

  lazy val audioRepository = new AudioRepository
  lazy val importService = new ImportService
  lazy val audioStorage = new AudioStorage
  lazy val ndlaClient = new NdlaClient
  lazy val migrationApiClient = new MigrationApiClient
  lazy val mappingApiClient = new MappingApiClient
  lazy val readService = new ReadService
  lazy val converterService = new ConverterService

  lazy val internController = new InternController
  lazy val resourcesApp = new ResourcesApp
  lazy val audioApiController = new AudioApiController
  lazy val healthController = new HealthController

  lazy val jestClient = JestClientFactory.getClient()
  lazy val elasticContentIndex = new ElasticContentIndex
  lazy val searchConverterService = new SearchConverterService
  lazy val searchIndexService = new SearchIndexService
  lazy val searchService = new SearchService
}
