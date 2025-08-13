/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import com.zaxxer.hikari.HikariDataSource
import no.ndla.audioapi.controller.{AudioController, HealthController, InternController, SeriesController}
import no.ndla.audioapi.model.api.ErrorHandling
import no.ndla.audioapi.repository.{AudioRepository, SeriesRepository}
import no.ndla.audioapi.service.*
import no.ndla.audioapi.service.search.*
import no.ndla.common.Clock
import no.ndla.common.aws.{NdlaAWSTranscribeClient, NdlaS3Client}
import no.ndla.common.brightcove.NdlaBrightcoveClient
import no.ndla.database.DataSource
import no.ndla.network.NdlaClient
import no.ndla.network.tapir.TapirApplication
import no.ndla.search.{BaseIndexService, Elastic4sClient, SearchLanguage}
import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment
    extends TapirApplication
    with DataSource
    with AudioRepository
    with SeriesRepository
    with NdlaClient
    with ReadService
    with WriteService
    with ValidationService
    with ConverterService
    with InternController
    with HealthController
    with NdlaS3Client
    with AudioController
    with SeriesController
    with Elastic4sClient
    with IndexService
    with BaseIndexService
    with SearchLanguage
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
    with TranscriptionService
    with NdlaAWSTranscribeClient
    with NdlaBrightcoveClient
    with ErrorHandling {
  override lazy val props: AudioApiProperties = new AudioApiProperties

  override lazy val dataSource: HikariDataSource       = mock[HikariDataSource]
  override lazy val audioRepository: AudioRepository   = mock[AudioRepository]
  override lazy val seriesRepository: SeriesRepository = mock[SeriesRepository]

  override lazy val s3Client: NdlaS3Client                    = mock[NdlaS3Client]
  override lazy val brightcoveClient: NdlaBrightcoveClient    = mock[NdlaBrightcoveClient]
  override lazy val transcribeClient: NdlaAWSTranscribeClient = mock[NdlaAWSTranscribeClient]

  override lazy val ndlaClient: NdlaClient           = mock[NdlaClient]
  override lazy val myndlaApiClient: MyNDLAApiClient = mock[MyNDLAApiClient]

  override lazy val readService: ReadService                   = mock[ReadService]
  override lazy val writeService: WriteService                 = mock[WriteService]
  override lazy val validationService: ValidationService       = mock[ValidationService]
  override lazy val converterService: ConverterService         = mock[ConverterService]
  override lazy val transcriptionService: TranscriptionService = mock[TranscriptionService]
  override lazy val s3TranscribeClient: NdlaS3Client           = mock[NdlaS3Client]

  override lazy val internController: InternController  = mock[InternController]
  override lazy val audioApiController: AudioController = mock[AudioController]
  override lazy val healthController: HealthController  = mock[HealthController]
  override lazy val seriesController: SeriesController  = mock[SeriesController]

  var e4sClient: NdlaE4sClient                                     = mock[NdlaE4sClient]
  override lazy val audioSearchService: AudioSearchService         = mock[AudioSearchService]
  override lazy val audioIndexService: AudioIndexService           = mock[AudioIndexService]
  override lazy val seriesSearchService: SeriesSearchService       = mock[SeriesSearchService]
  override lazy val seriesIndexService: SeriesIndexService         = mock[SeriesIndexService]
  override lazy val tagSearchService: TagSearchService             = mock[TagSearchService]
  override lazy val tagIndexService: TagIndexService               = mock[TagIndexService]
  override lazy val searchConverterService: SearchConverterService = mock[SearchConverterService]

  override lazy val clock: SystemClock = mock[SystemClock]
  def services: List[TapirController]  = List.empty
  val swagger: SwaggerController       = mock[SwaggerController]

}
