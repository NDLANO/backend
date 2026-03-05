/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import no.ndla.audioapi.controller.{AudioController, ControllerErrorHandling, InternController, SeriesController}
import no.ndla.audioapi.integration.{NDLAS3Client, TranscribeS3Client}
import no.ndla.audioapi.model.domain.{DBAudioMetaInformation, DBSeries}
import no.ndla.audioapi.repository.{AudioRepository, SeriesRepository}
import no.ndla.audioapi.service.*
import no.ndla.audioapi.service.search.*
import no.ndla.common.Clock
import no.ndla.common.aws.NdlaAWSTranscribeClient
import no.ndla.common.brightcove.NdlaBrightcoveClient
import no.ndla.database.{DBMigrator, DataSource, DBUtility}
import no.ndla.network.NdlaClient
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.tapir.{
  ErrorHelpers,
  Routes,
  SwaggerController,
  TapirApplication,
  TapirController,
  TapirHealthController,
}
import no.ndla.search.{NdlaE4sClient, SearchLanguage}
import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment extends TapirApplication[AudioApiProperties] with MockitoSugar {
  implicit lazy val props: AudioApiProperties = new AudioApiProperties

  implicit lazy val migrator: DBMigrator                   = mock[DBMigrator]
  implicit lazy val errorHelpers: ErrorHelpers             = new ErrorHelpers
  implicit lazy val errorHandling: ControllerErrorHandling = new ControllerErrorHandling
  implicit lazy val clock: Clock                           = mock[Clock]
  implicit lazy val routes: Routes                         = mock[Routes]
  implicit lazy val services: List[TapirController]        = List.empty
  implicit lazy val searchLanguage: SearchLanguage         = mock[SearchLanguage]

  implicit lazy val dataSource: DataSource                         = mock[DataSource]
  implicit lazy val audioRepository: AudioRepository               = mock[AudioRepository]
  implicit lazy val seriesRepository: SeriesRepository             = mock[SeriesRepository]
  implicit lazy val dbUtility: DBUtility                           = new DBUtility
  implicit lazy val dbAudioMetaInformation: DBAudioMetaInformation = new DBAudioMetaInformation
  implicit lazy val dbSeries: DBSeries                             = new DBSeries

  implicit lazy val s3Client: NDLAS3Client                    = mock[NDLAS3Client]
  implicit lazy val brightcoveClient: NdlaBrightcoveClient    = mock[NdlaBrightcoveClient]
  implicit lazy val transcribeClient: NdlaAWSTranscribeClient = mock[NdlaAWSTranscribeClient]

  implicit lazy val ndlaClient: NdlaClient           = mock[NdlaClient]
  implicit lazy val myndlaApiClient: MyNDLAApiClient = mock[MyNDLAApiClient]

  implicit lazy val readService: ReadService                   = mock[ReadService]
  implicit lazy val writeService: WriteService                 = mock[WriteService]
  implicit lazy val validationService: ValidationService       = mock[ValidationService]
  implicit lazy val converterService: ConverterService         = mock[ConverterService]
  implicit lazy val transcriptionService: TranscriptionService = mock[TranscriptionService]
  implicit lazy val s3TranscribeClient: TranscribeS3Client     = mock[TranscribeS3Client]

  implicit lazy val internController: InternController      = mock[InternController]
  implicit lazy val audioApiController: AudioController     = mock[AudioController]
  implicit lazy val healthController: TapirHealthController = mock[TapirHealthController]
  implicit lazy val seriesController: SeriesController      = mock[SeriesController]

  implicit lazy val e4sClient: NdlaE4sClient                       = mock[NdlaE4sClient]
  implicit lazy val audioSearchService: AudioSearchService         = mock[AudioSearchService]
  implicit lazy val audioIndexService: AudioIndexService           = mock[AudioIndexService]
  implicit lazy val seriesSearchService: SeriesSearchService       = mock[SeriesSearchService]
  implicit lazy val seriesIndexService: SeriesIndexService         = mock[SeriesIndexService]
  implicit lazy val tagSearchService: TagSearchService             = mock[TagSearchService]
  implicit lazy val tagIndexService: TagIndexService               = mock[TagIndexService]
  implicit lazy val searchConverterService: SearchConverterService = mock[SearchConverterService]

  implicit lazy val swagger: SwaggerController = mock[SwaggerController]

}
