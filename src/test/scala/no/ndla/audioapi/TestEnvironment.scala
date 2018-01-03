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
import no.ndla.audioapi.auth.{Role, User}
import no.ndla.audioapi.controller.{AudioController, HealthController, InternController}
import no.ndla.audioapi.integration._
import no.ndla.audioapi.repository.AudioRepository
import no.ndla.audioapi.service._
import no.ndla.audioapi.service.search._
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
  with DraftApiClient
  with ValidationService
  with ConverterService
  with AudioStorageService
  with InternController
  with HealthController
  with AudioController
  with ElasticClient
  with Elastic4sClient
  with IndexService
  with SearchConverterService
  with SearchIndexService
  with SearchService
  with TagsService
  with MockitoSugar
  with User
  with Role
  with Clock
{
  val dataSource = mock[sql.DataSource]
  val storageName = AudioApiProperties.StorageName
  val audioStorage = mock[AudioStorage]
  val audioRepository = mock[AudioRepository]

  val amazonClient = mock[AmazonS3Client]
  val ndlaClient = mock[NdlaClient]
  val migrationApiClient = mock[MigrationApiClient]
  val draftApiClient = mock[DraftApiClient]

  val importService = mock[ImportService]
  val readService = mock[ReadService]
  val writeService = mock[WriteService]
  val validationService = mock[ValidationService]
  val converterService = mock[ConverterService]
  val tagsService = mock[TagsService]

  val internController = mock[InternController]
  val resourcesApp = mock[ResourcesApp]
  val audioApiController = mock[AudioController]
  val healthController = mock[HealthController]

  val jestClient = mock[NdlaJestClient]
  val e4sClient = mock[NdlaE4sClient]
  val searchService = mock[SearchService]
  val indexService = mock[IndexService]
  val searchConverterService = mock[SearchConverterService]
  val searchIndexService = mock[SearchIndexService]

  val clock = mock[SystemClock]
  val authUser = mock[AuthUser]
  val authRole = new AuthRole

}
