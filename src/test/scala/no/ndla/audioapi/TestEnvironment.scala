/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import javax.sql

import com.amazonaws.services.s3.AmazonS3Client
import io.searchbox.client.JestClient
import no.ndla.audioapi.controller.{AudioApiController, HealthController, InternController}
import no.ndla.audioapi.integration._
import no.ndla.audioapi.repository.AudioRepository
import no.ndla.audioapi.service.search._
import no.ndla.audioapi.service._
import no.ndla.network.NdlaClient
import org.scalatest.mockito.MockitoSugar

trait TestEnvironment
  extends DataSource
  with AudioRepository
  with NdlaClient
  with MigrationApiClient
  with ImportService
  with AmazonClient
  with ReadService
  with WriteService
  with ConverterService
  with AudioStorageService
  with InternController
  with HealthController
  with AudioApiController
  with ElasticClient
  with ElasticIndexService
  with SearchConverterService
  with SearchIndexService
  with SearchService
  with TagsService
  with MockitoSugar
{
  val dataSource = mock[sql.DataSource]
  val storageName = AudioApiProperties.StorageName
  val audioStorage = mock[AudioStorage]
  val audioRepository = mock[AudioRepository]

  val amazonClient = mock[AmazonS3Client]
  val ndlaClient = mock[NdlaClient]
  val migrationApiClient = mock[MigrationApiClient]

  val importService = mock[ImportService]
  val readService = mock[ReadService]
  val writeService = mock[WriteService]
  val converterService = mock[ConverterService]
  val tagsService = mock[TagsService]

  val internController = mock[InternController]
  val resourcesApp = mock[ResourcesApp]
  val audioApiController = mock[AudioApiController]
  val healthController = mock[HealthController]

  val jestClient = mock[NdlaJestClient]
  val searchService = mock[SearchService]
  val elasticIndexService = mock[ElasticIndexService]
  val searchConverterService = mock[SearchConverterService]
  val searchIndexService = mock[SearchIndexService]
}
