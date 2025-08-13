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

class ComponentRegistry(properties: AudioApiProperties)
    extends BaseComponentRegistry[AudioApiProperties]
    with TapirApplication
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
    with AudioController
    with SeriesController
    with TranscriptionController
    with SearchService
    with AudioSearchService
    with SeriesSearchService
    with TagSearchService
    with Elastic4sClient
    with IndexService
    with BaseIndexService
    with SearchLanguage
    with AudioIndexService
    with SeriesIndexService
    with TagIndexService
    with SearchConverterService
    with Clock
    with Props
    with DBMigrator
    with ErrorHandling
    with SwaggerDocControllerConfig
    with NdlaS3Client
    with TranscriptionService
    with NdlaAWSTranscribeClient
    with NdlaBrightcoveClient {
  override lazy val props: AudioApiProperties = properties
  override lazy val migrator: DBMigrator      = DBMigrator(
    new V5__AddAgreementToAudio,
    new V6__TranslateUntranslatedAuthors
  )
  override lazy val dataSource: HikariDataSource = DataSource.getHikariDataSource

  override lazy val s3Client           = new NdlaS3Client(props.StorageName, props.StorageRegion)
  override lazy val s3TranscribeClient = new NdlaS3Client(props.TranscribeStorageName, props.TranscribeStorageRegion)
  override lazy val brightcoveClient   = new NdlaBrightcoveClient()
  override lazy val transcribeClient   = new NdlaAWSTranscribeClient(props.TranscribeStorageRegion)

  override lazy val audioRepository  = new AudioRepository
  override lazy val seriesRepository = new SeriesRepository

  override lazy val ndlaClient                       = new NdlaClient
  override lazy val myndlaApiClient: MyNDLAApiClient = new MyNDLAApiClient

  override lazy val readService          = new ReadService
  override lazy val writeService         = new WriteService
  override lazy val validationService    = new ValidationService
  override lazy val converterService     = new ConverterService
  override lazy val transcriptionService = new TranscriptionService

  override lazy val internController        = new InternController
  override lazy val audioApiController      = new AudioController
  override lazy val seriesController        = new SeriesController
  override lazy val healthController        = new HealthController
  override lazy val transcriptionController = new TranscriptionController

  var e4sClient: NdlaE4sClient             = Elastic4sClientFactory.getClient(props.SearchServer)
  override lazy val searchConverterService = new SearchConverterService
  override lazy val audioIndexService      = new AudioIndexService
  override lazy val audioSearchService     = new AudioSearchService
  override lazy val seriesIndexService     = new SeriesIndexService
  override lazy val seriesSearchService    = new SeriesSearchService
  override lazy val tagIndexService        = new TagIndexService
  override lazy val tagSearchService       = new TagSearchService

  override lazy val clock = new SystemClock

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
