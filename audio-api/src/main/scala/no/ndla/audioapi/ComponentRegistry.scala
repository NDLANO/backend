/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import cats.data.Kleisli
import cats.effect.IO
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.zaxxer.hikari.HikariDataSource
import no.ndla.audioapi.controller._
import no.ndla.audioapi.integration._
import no.ndla.audioapi.model.api.ErrorHelpers
import no.ndla.audioapi.model.domain.{DBAudioMetaInformation, DBSeries}
import no.ndla.audioapi.repository.{AudioRepository, SeriesRepository}
import no.ndla.audioapi.service._
import no.ndla.audioapi.service.search._
import no.ndla.common.Clock
import no.ndla.common.configuration.BaseComponentRegistry
import no.ndla.network.NdlaClient
import no.ndla.network.tapir.{NdlaMiddleware, Routes, Service, SwaggerControllerConfig, TapirHealthController}
import no.ndla.search.{BaseIndexService, Elastic4sClient}
import org.http4s.{Request, Response}

class ComponentRegistry(properties: AudioApiProperties)
    extends BaseComponentRegistry[AudioApiProperties]
    with DataSource
    with AudioRepository
    with SeriesRepository
    with DBSeries
    with DBAudioMetaInformation
    with NdlaClient
    with AmazonClient
    with ReadService
    with WriteService
    with DraftApiClient
    with ValidationService
    with ConverterService
    with AudioStorageService
    with InternController
    with Service
    with NdlaMiddleware
    with HealthController
    with TapirHealthController
    with AudioController
    with SeriesController
    with SearchService
    with AudioSearchService
    with SeriesSearchService
    with TagSearchService
    with Elastic4sClient
    with IndexService
    with BaseIndexService
    with AudioIndexService
    with SeriesIndexService
    with TagIndexService
    with SearchConverterService
    with SwaggerControllerConfig
    with Clock
    with Routes
    with Props
    with DBMigrator
    with ErrorHelpers
    with SwaggerDocControllerConfig {
  override val props: AudioApiProperties    = properties
  override val migrator: DBMigrator         = new DBMigrator
  override val dataSource: HikariDataSource = DataSource.getHikariDataSource
  DataSource.connectToDatabase()

  val currentRegion: Option[Regions] = Option(Regions.getCurrentRegion).map(region => Regions.fromName(region.getName))

  val amazonClient: AmazonS3 =
    AmazonS3ClientBuilder
      .standard()
      .withRegion(currentRegion.getOrElse(Regions.EU_CENTRAL_1))
      .build()

  lazy val audioRepository  = new AudioRepository
  lazy val seriesRepository = new SeriesRepository
  lazy val audioStorage     = new AudioStorage

  lazy val ndlaClient     = new NdlaClient
  lazy val draftApiClient = new DraftApiClient

  lazy val readService       = new ReadService
  lazy val writeService      = new WriteService
  lazy val validationService = new ValidationService
  lazy val converterService  = new ConverterService

  lazy val internController   = new InternController
  lazy val audioApiController = new AudioController
  lazy val seriesController   = new SeriesController
  lazy val healthController   = new HealthController

  var e4sClient: NdlaE4sClient    = Elastic4sClientFactory.getClient(props.SearchServer)
  lazy val searchConverterService = new SearchConverterService
  lazy val audioIndexService      = new AudioIndexService
  lazy val audioSearchService     = new AudioSearchService
  lazy val seriesIndexService     = new SeriesIndexService
  lazy val seriesSearchService    = new SeriesSearchService
  lazy val tagIndexService        = new TagIndexService
  lazy val tagSearchService       = new TagSearchService

  lazy val clock = new SystemClock

  private val services: List[Service] = List(
    audioApiController,
    seriesController,
    internController,
    healthController
  )

  private val swaggerDocController = new SwaggerController(services, SwaggerDocControllerConfig.swaggerInfo)

  def routes: Kleisli[IO, Request[IO], Response[IO]] = Routes.build(services :+ swaggerDocController)

}
