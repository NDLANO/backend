/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import com.amazonaws.services.s3.AmazonS3Client
import com.zaxxer.hikari.HikariDataSource
import no.ndla.audioapi.auth.{Role, User}
import no.ndla.audioapi.controller.{AudioController, HealthController, InternController}
import no.ndla.audioapi.integration._
import no.ndla.audioapi.repository.AudioRepository
import no.ndla.audioapi.service._
import no.ndla.audioapi.service.search._
import no.ndla.network.NdlaClient
import org.mockito.scalatest.MockitoSugar

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
    with Elastic4sClient
    with IndexService
    with AudioIndexService
    with SearchConverterService
    with SearchService
    with AudioSearchService
    with TagIndexService
    with TagSearchService
    with TagsService
    with MockitoSugar
    with User
    with Role
    with Clock {
  val dataSource: HikariDataSource = mock[HikariDataSource]
  val storageName: String = AudioApiProperties.StorageName
  val audioStorage: AudioStorage = mock[AudioStorage]
  val audioRepository: AudioRepository = mock[AudioRepository]

  val amazonClient: AmazonS3Client = mock[AmazonS3Client]
  val ndlaClient: NdlaClient = mock[NdlaClient]
  val migrationApiClient: MigrationApiClient = mock[MigrationApiClient]
  val draftApiClient: DraftApiClient = mock[DraftApiClient]

  val importService: ImportService = mock[ImportService]
  val readService: ReadService = mock[ReadService]
  val writeService: WriteService = mock[WriteService]
  val validationService: ValidationService = mock[ValidationService]
  val converterService: ConverterService = mock[ConverterService]
  val tagsService: TagsService = mock[TagsService]

  val internController: InternController = mock[InternController]
  val resourcesApp: ResourcesApp = mock[ResourcesApp]
  val audioApiController: AudioController = mock[AudioController]
  val healthController: HealthController = mock[HealthController]

  val e4sClient: NdlaE4sClient = mock[NdlaE4sClient]
  val audioSearchService: AudioSearchService = mock[AudioSearchService]
  val audioIndexService: AudioIndexService = mock[AudioIndexService]
  val tagSearchService: TagSearchService = mock[TagSearchService]
  val tagIndexService: TagIndexService = mock[TagIndexService]
  val searchConverterService: SearchConverterService = mock[SearchConverterService]

  val clock: SystemClock = mock[SystemClock]
  val authUser: AuthUser = mock[AuthUser]
  val authRole = new AuthRole

}
