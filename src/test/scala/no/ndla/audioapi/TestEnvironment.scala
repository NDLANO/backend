/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import javax.sql.DataSource

import com.amazonaws.services.s3.AmazonS3Client
import io.searchbox.client.JestClient
import no.ndla.audioapi.controller.{AudioApiController, HealthController, InternController}
import no.ndla.audioapi.integration._
import no.ndla.audioapi.repository.AudioRepositoryComponent
import no.ndla.audioapi.service.search.{ElasticContentIndexComponent, SearchConverterService, SearchIndexServiceComponent, SearchService}
import no.ndla.audioapi.service.{AudioStorageService, ConverterService, ImportServiceComponent, ReadServiceComponent}
import no.ndla.network.NdlaClient
import org.scalatest.mock.MockitoSugar

trait TestEnvironment
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
  with ElasticClientComponent
  with ElasticContentIndexComponent
  with SearchConverterService
  with SearchIndexServiceComponent
  with SearchService
  with MockitoSugar
{
  val dataSource = mock[DataSource]
  val amazonClient = mock[AmazonS3Client]
  val storageName = AudioApiProperties.StorageName
  val audioRepository = mock[AudioRepository]
  val importService = mock[ImportService]
  val audioStorage = mock[AudioStorage]
  val ndlaClient = mock[NdlaClient]
  val migrationApiClient = mock[MigrationApiClient]
  val mappingApiClient = mock[MappingApiClient]
  val readService = mock[ReadService]
  val converterService = mock[ConverterService]

  val internController = mock[InternController]
  val resourcesApp = mock[ResourcesApp]
  val audioApiController = mock[AudioApiController]
  val healthController = mock[HealthController]

  val jestClient = mock[JestClient]
  val searchService = mock[SearchService]
  val elasticContentIndex = mock[ElasticContentIndex]
  val searchConverterService = mock[SearchConverterService]
  val searchIndexService = mock[SearchIndexService]
}
