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
import no.ndla.audioapi.controller.{AudioController, HealthController, InternController, SeriesController}
import no.ndla.audioapi.integration._
import no.ndla.audioapi.model.api.ErrorHelpers
import no.ndla.audioapi.repository.{AudioRepository, SeriesRepository}
import no.ndla.audioapi.service._
import no.ndla.audioapi.service.search._
import no.ndla.common.Clock
import no.ndla.network.NdlaClient
import no.ndla.network.tapir.{NdlaMiddleware, Routes, Service, TapirHealthController}
import no.ndla.search.{BaseIndexService, Elastic4sClient}
import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment
    extends DataSource
    with AudioRepository
    with SeriesRepository
    with NdlaClient
    with AmazonClient
    with ReadService
    with WriteService
    with ValidationService
    with ConverterService
    with AudioStorageService
    with InternController
    with Routes[Eff]
    with NdlaMiddleware
    with HealthController
    with TapirHealthController
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
    with Clock
    with Props
    with ErrorHelpers {
  override val props: AudioApiProperties = new AudioApiProperties

  val dataSource: HikariDataSource       = mock[HikariDataSource]
  val storageName: String                = props.StorageName
  val audioStorage: AudioStorage         = mock[AudioStorage]
  val audioRepository: AudioRepository   = mock[AudioRepository]
  val seriesRepository: SeriesRepository = mock[SeriesRepository]

  val amazonClient: AmazonS3Client = mock[AmazonS3Client]
  val ndlaClient: NdlaClient       = mock[NdlaClient]

  val readService: ReadService             = mock[ReadService]
  val writeService: WriteService           = mock[WriteService]
  val validationService: ValidationService = mock[ValidationService]
  val converterService: ConverterService   = mock[ConverterService]

  val internController: InternController  = mock[InternController]
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

  val clock: SystemClock           = mock[SystemClock]
  def services: List[Service[Eff]] = List.empty

}
