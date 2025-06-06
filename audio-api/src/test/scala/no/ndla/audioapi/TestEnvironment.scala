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

  val dataSource: HikariDataSource       = mock[HikariDataSource]
  val audioRepository: AudioRepository   = mock[AudioRepository]
  val seriesRepository: SeriesRepository = mock[SeriesRepository]

  val s3Client: NdlaS3Client                    = mock[NdlaS3Client]
  val brightcoveClient: NdlaBrightcoveClient    = mock[NdlaBrightcoveClient]
  val transcribeClient: NdlaAWSTranscribeClient = mock[NdlaAWSTranscribeClient]

  val ndlaClient: NdlaClient           = mock[NdlaClient]
  val myndlaApiClient: MyNDLAApiClient = mock[MyNDLAApiClient]

  val readService: ReadService                   = mock[ReadService]
  val writeService: WriteService                 = mock[WriteService]
  val validationService: ValidationService       = mock[ValidationService]
  val converterService: ConverterService         = mock[ConverterService]
  val transcriptionService: TranscriptionService = mock[TranscriptionService]
  val s3TranscribeClient: NdlaS3Client           = mock[NdlaS3Client]

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

  val clock: SystemClock              = mock[SystemClock]
  def services: List[TapirController] = List.empty
  val swagger: SwaggerController      = mock[SwaggerController]

}
