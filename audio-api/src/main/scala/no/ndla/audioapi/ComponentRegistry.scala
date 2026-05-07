/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import no.ndla.audioapi.controller.*
import no.ndla.audioapi.db.migrationwithdependencies.{V5__AddAgreementToAudio, V6__TranslateUntranslatedAuthors}
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
import no.ndla.network.tapir.{ErrorHelpers, Routes, SwaggerController, SwaggerInfo, TapirApplication}
import no.ndla.search.{Elastic4sClientFactory, NdlaE4sClient, SearchLanguage}

class ComponentRegistry(properties: AudioApiProperties) extends TapirApplication[AudioApiProperties] {
  given props: AudioApiProperties                      = properties
  given dataSource: DataSource                         = DataSource.getDataSource
  given clock: Clock                                   = new Clock
  given migrator: DBMigrator                           = DBMigrator(new V5__AddAgreementToAudio, new V6__TranslateUntranslatedAuthors)
  given errorHelpers: ErrorHelpers                     = new ErrorHelpers
  given errorHandling: ControllerErrorHandling         = new ControllerErrorHandling
  given searchLanguage: SearchLanguage                 = new SearchLanguage
  given dbUtility: DBUtility                           = new DBUtility
  given dbAudioMetaInformation: DBAudioMetaInformation = new DBAudioMetaInformation
  given dbSeries: DBSeries                             = new DBSeries

  given s3Client: NDLAS3Client                 = new NDLAS3Client(props.StorageName, props.StorageRegion)
  given s3TranscribeClient: TranscribeS3Client =
    new TranscribeS3Client(props.TranscribeStorageName, props.TranscribeStorageRegion)
  given brightcoveClient: NdlaBrightcoveClient    = new NdlaBrightcoveClient
  given transcribeClient: NdlaAWSTranscribeClient = new NdlaAWSTranscribeClient(props.TranscribeStorageRegion)

  given audioRepository: AudioRepository   = new AudioRepository
  given seriesRepository: SeriesRepository = new SeriesRepository

  given ndlaClient: NdlaClient           = new NdlaClient
  given myndlaApiClient: MyNDLAApiClient = new MyNDLAApiClient

  given converterService: ConverterService         = new ConverterService
  given validationService: ValidationService       = new ValidationService
  given transcriptionService: TranscriptionService = new TranscriptionService

  given e4sClient: NdlaE4sClient                       = Elastic4sClientFactory.getClient(props.SearchServer)
  given searchConverterService: SearchConverterService = new SearchConverterService
  given seriesIndexService: SeriesIndexService         = new SeriesIndexService
  given seriesSearchService: SeriesSearchService       = new SeriesSearchService
  given audioIndexService: AudioIndexService           = new AudioIndexService
  given audioSearchService: AudioSearchService         = new AudioSearchService
  given tagIndexService: TagIndexService               = new TagIndexService
  given tagSearchService: TagSearchService             = new TagSearchService

  given readService: ReadService   = new ReadService
  given writeService: WriteService = new WriteService

  given internController: InternController               = new InternController
  given audioApiController: AudioController              = new AudioController
  given seriesController: SeriesController               = new SeriesController
  given healthController: HealthController               = new HealthController
  given transcriptionController: TranscriptionController = new TranscriptionController

  given swaggerInfo: SwaggerInfo = SwaggerInfo(
    prefix = "audio-api",
    description = "Searching and fetching all audio used in the NDLA platform.\n\n" +
      "The Audio API provides an endpoint for searching and fetching audio used in NDLA resources. " +
      "Meta-data like title, tags, language and license are searchable and also provided in the results. " +
      "The media file is provided as an URL with the mime type.",
  )
  given swagger: SwaggerController = new SwaggerController(
    internController,
    audioApiController,
    seriesController,
    healthController,
    transcriptionController,
  )

  given services: List[no.ndla.network.tapir.TapirController] = swagger.allServices
  given routes: Routes                                        = new Routes
}
