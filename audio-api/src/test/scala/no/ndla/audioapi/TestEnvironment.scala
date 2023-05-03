/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import com.amazonaws.services.s3.AmazonS3Client
import com.zaxxer.hikari.HikariDataSource
import no.ndla.audioapi.auth.{Role, User}
import no.ndla.audioapi.controller.{AudioController, HealthController, InternController, SeriesController}
import no.ndla.audioapi.integration._
import no.ndla.audioapi.model.api.ErrorHelpers
import no.ndla.audioapi.model.domain.{DBAudioMetaInformation, DBSeries}
import no.ndla.audioapi.repository.{AudioRepository, SeriesRepository}
import no.ndla.audioapi.service._
import no.ndla.audioapi.service.search._
import no.ndla.common.Clock
import no.ndla.network.NdlaClient
import no.ndla.network.scalatra.{NdlaControllerBase, NdlaSwaggerSupport}
import no.ndla.network.tapir.{NdlaMiddleware, Service}
import no.ndla.search.{BaseIndexService, Elastic4sClient, NdlaE4sClient}
import org.mockito.scalatest.MockitoSugar

trait TestEnvironment
    extends DataSource
    with DBAudioMetaInformation
    with DBSeries
    with AudioRepository
    with SeriesRepository
    with NdlaClient
    with AmazonClient
    with ReadService
    with WriteService
    with DraftApiClient
    with ValidationService
    with ConverterService
    with AudioStorageService
    with NdlaControllerBase
    with NdlaSwaggerSupport
    with InternController
    with Service
    with NdlaMiddleware
    with HealthController
    with AudioController
    with SeriesController
    with Elastic4sClient
    with IndexService
    with BaseIndexService
    with AudioIndexService
    with SeriesIndexService
    with SearchConverterService
    with SearchService
    with AudioSearchService
    with SeriesSearchService
    with TagIndexService
    with TagSearchService
    with MockitoSugar
    with User
    with Role
    with Clock
    with Props
    with AudioApiInfo
    with ErrorHelpers {
  override val props: AudioApiProperties = new AudioApiProperties

  val dataSource: HikariDataSource       = mock[HikariDataSource]
  val storageName: String                = props.StorageName
  val audioStorage: AudioStorage         = mock[AudioStorage]
  val audioRepository: AudioRepository   = mock[AudioRepository]
  val seriesRepository: SeriesRepository = mock[SeriesRepository]

  val amazonClient: AmazonS3Client   = mock[AmazonS3Client]
  val ndlaClient: NdlaClient         = mock[NdlaClient]
  val draftApiClient: DraftApiClient = mock[DraftApiClient]

  val readService: ReadService             = mock[ReadService]
  val writeService: WriteService           = mock[WriteService]
  val validationService: ValidationService = mock[ValidationService]
  val converterService: ConverterService   = mock[ConverterService]

  val internController: InternController  = mock[InternController]
  val resourcesApp: ResourcesApp          = mock[ResourcesApp]
  val audioApiController: AudioController = mock[AudioController]
  val healthController: HealthController  = mock[HealthController]
  val seriesController: SeriesController  = mock[SeriesController]

  var e4sClient: NdlaE4sClient                       = mock[NdlaE4sClient]
  val audioSearchService: AudioSearchService         = mock[AudioSearchService]
  val audioIndexService: AudioIndexService           = mock[AudioIndexService]
  val seriesSearchService: SeriesSearchService       = mock[SeriesSearchService]
  val seriesIndexService: SeriesIndexService         = mock[SeriesIndexService]
  val tagSearchService: TagSearchService             = mock[TagSearchService]
  val tagIndexService: TagIndexService               = mock[TagIndexService]
  val searchConverterService: SearchConverterService = mock[SearchConverterService]

  val clock: SystemClock = mock[SystemClock]
  val authUser: AuthUser = mock[AuthUser]
  val authRole           = new AuthRole

}
