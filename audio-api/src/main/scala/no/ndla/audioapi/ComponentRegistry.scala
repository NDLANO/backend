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
  override val props: AudioApiProperties = properties
  override val migrator: DBMigrator = DBMigrator(
    new V5__AddAgreementToAudio,
    new V6__TranslateUntranslatedAuthors
  )
  override val dataSource: HikariDataSource = DataSource.getHikariDataSource
  DataSource.connectToDatabase()

  lazy val s3Client           = new NdlaS3Client(props.StorageName, props.StorageRegion)
  lazy val s3TranscribeClient = new NdlaS3Client(props.TranscribeStorageName, props.TranscribeStorageRegion)
  lazy val brightcoveClient   = new NdlaBrightcoveClient()
  lazy val transcribeClient   = new NdlaAWSTranscribeClient(props.TranscribeStorageRegion)

  lazy val audioRepository  = new AudioRepository
  lazy val seriesRepository = new SeriesRepository

  lazy val ndlaClient                       = new NdlaClient
  lazy val myndlaApiClient: MyNDLAApiClient = new MyNDLAApiClient

  lazy val readService          = new ReadService
  lazy val writeService         = new WriteService
  lazy val validationService    = new ValidationService
  lazy val converterService     = new ConverterService
  lazy val transcriptionService = new TranscriptionService

  lazy val internController        = new InternController
  lazy val audioApiController      = new AudioController
  lazy val seriesController        = new SeriesController
  lazy val healthController        = new HealthController
  lazy val transcriptionController = new TranscriptionController

  var e4sClient: NdlaE4sClient    = Elastic4sClientFactory.getClient(props.SearchServer)
  lazy val searchConverterService = new SearchConverterService
  lazy val audioIndexService      = new AudioIndexService
  lazy val audioSearchService     = new AudioSearchService
  lazy val seriesIndexService     = new SeriesIndexService
  lazy val seriesSearchService    = new SeriesSearchService
  lazy val tagIndexService        = new TagIndexService
  lazy val tagSearchService       = new TagSearchService

  lazy val clock = new SystemClock

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
