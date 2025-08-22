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
    with MockitoSugar {
  given props: AudioApiProperties = new AudioApiProperties

  given dataSource: HikariDataSource       = mock[HikariDataSource]
  given audioRepository: AudioRepository   = mock[AudioRepository]
  given seriesRepository: SeriesRepository = mock[SeriesRepository]

  given s3Client: NdlaS3Client                    = mock[NdlaS3Client]
  given brightcoveClient: NdlaBrightcoveClient    = mock[NdlaBrightcoveClient]
  given transcribeClient: NdlaAWSTranscribeClient = mock[NdlaAWSTranscribeClient]

  given ndlaClient: NdlaClient           = mock[NdlaClient]
  given myndlaApiClient: MyNDLAApiClient = mock[MyNDLAApiClient]

  given readService: ReadService                   = mock[ReadService]
  given writeService: WriteService                 = mock[WriteService]
  given validationService: ValidationService       = mock[ValidationService]
  given converterService: ConverterService         = mock[ConverterService]
  given transcriptionService: TranscriptionService = mock[TranscriptionService]
  given s3TranscribeClient: NdlaS3Client           = mock[NdlaS3Client]

  given internController: InternController  = mock[InternController]
  given audioApiController: AudioController = mock[AudioController]
  given healthController: HealthController  = mock[HealthController]
  given seriesController: SeriesController  = mock[SeriesController]

  given e4sClient: NdlaE4sClient                                     = mock[NdlaE4sClient]
  given audioSearchService: AudioSearchService         = mock[AudioSearchService]
  given audioIndexService: AudioIndexService           = mock[AudioIndexService]
  given seriesSearchService: SeriesSearchService       = mock[SeriesSearchService]
  given seriesIndexService: SeriesIndexService         = mock[SeriesIndexService]
  given tagSearchService: TagSearchService             = mock[TagSearchService]
  given tagIndexService: TagIndexService               = mock[TagIndexService]
  given searchConverterService: SearchConverterService = mock[SearchConverterService]

  given clock: SystemClock = mock[SystemClock]
  def services: List[TapirController]  = List.empty
  val swagger: SwaggerController       = mock[SwaggerController]

}
