/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import com.zaxxer.hikari.HikariDataSource
import no.ndla.audioapi.controller.*
import no.ndla.audioapi.db.migrationwithdependencies.{V5__AddAgreementToAudio, V6__TranslateUntranslatedAuthors}
import no.ndla.audioapi.model.api.ErrorHandling
import no.ndla.audioapi.repository.{AudioRepository, SeriesRepository}
import no.ndla.audioapi.service.*
import no.ndla.audioapi.service.search.*
import no.ndla.common.Clock
import no.ndla.common.aws.{NdlaAWSTranscribeClient, NdlaS3Client}
import no.ndla.common.brightcove.NdlaBrightcoveClient
import no.ndla.common.configuration.BaseComponentRegistry
import no.ndla.database.{DBMigrator, DataSource}
import no.ndla.network.NdlaClient
import no.ndla.network.tapir.TapirApplication
import no.ndla.search.{BaseIndexService, Elastic4sClient, SearchLanguage}

class ComponentRegistry(properties: AudioApiProperties) extends TapirApplication[AudioApiProperties] {
  given props: AudioApiProperties = properties
  given migrator: DBMigrator      = DBMigrator(
    new V5__AddAgreementToAudio,
    new V6__TranslateUntranslatedAuthors
  )
  given dataSource: HikariDataSource = DataSource.getDataSource

  given s3Client           = new NdlaS3Client(props.StorageName, props.StorageRegion)
  given s3TranscribeClient = new NdlaS3Client(props.TranscribeStorageName, props.TranscribeStorageRegion)
  given brightcoveClient   = new NdlaBrightcoveClient()
  given transcribeClient   = new NdlaAWSTranscribeClient(props.TranscribeStorageRegion)

  given audioRepository  = new AudioRepository
  given seriesRepository = new SeriesRepository

  given ndlaClient                       = new NdlaClient
  given myndlaApiClient: MyNDLAApiClient = new MyNDLAApiClient

  given readService          = new ReadService
  given writeService         = new WriteService
  given validationService    = new ValidationService
  given converterService     = new ConverterService
  given transcriptionService = new TranscriptionService

  given internController        = new InternController
  given audioApiController      = new AudioController
  given seriesController        = new SeriesController
  given healthController        = new HealthController
  given transcriptionController = new TranscriptionController

  var e4sClient: NdlaE4sClient = Elastic4sClientFactory.getClient(props.SearchServer)
  given searchConverterService = new SearchConverterService
  given audioIndexService      = new AudioIndexService
  given audioSearchService     = new AudioSearchService
  given seriesIndexService     = new SeriesIndexService
  given seriesSearchService    = new SeriesSearchService
  given tagIndexService        = new TagIndexService
  given tagSearchService       = new TagSearchService

  given clock = new SystemClock

  val swagger = new SwaggerController(
    List(
      audioApiController,
      seriesController,
      internController,
      healthController,
      transcriptionController
    ),
    SwaggerDocControllerConfig.swaggerInfo
  )

  override def services: List[TapirController] = swagger.getServices()
}
