/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.zaxxer.hikari.HikariDataSource
import no.ndla.audioapi.auth.{Role, User}
import no.ndla.audioapi.controller.{AudioController, HealthController, InternController, SeriesController}
import no.ndla.audioapi.integration._
import no.ndla.audioapi.repository.{AudioRepository, SeriesRepository}
import no.ndla.audioapi.service.search.{AudioIndexService, _}
import no.ndla.audioapi.service._
import no.ndla.network.NdlaClient
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

object ComponentRegistry
    extends DataSource
    with AudioRepository
    with SeriesRepository
    with NdlaClient
    with MigrationApiClient
    with ImportService
    with TagsService
    with AmazonClient
    with ReadService
    with WriteService
    with DraftApiClient
    with ValidationService
    with ConverterService
    with AudioStorageService
    with InternController
    with HealthController
    with AudioController
    with SeriesController
    with SearchService
    with AudioSearchService
    with SeriesSearchService
    with TagSearchService
    with Elastic4sClient
    with IndexService
    with AudioIndexService
    with SeriesIndexService
    with TagIndexService
    with SearchConverterService
    with User
    with Role
    with Clock {
  def connectToDatabase(): Unit = ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))
  implicit val swagger: AudioSwagger = new AudioSwagger

  lazy val dataSource: HikariDataSource = DataSource.getHikariDataSource
  connectToDatabase()

  val currentRegion: Option[Regions] = Option(Regions.getCurrentRegion).map(region => Regions.fromName(region.getName))

  val amazonClient: AmazonS3 =
    AmazonS3ClientBuilder
      .standard()
      .withRegion(currentRegion.getOrElse(Regions.EU_CENTRAL_1))
      .build()

  lazy val storageName: String = AudioApiProperties.StorageName

  lazy val audioRepository = new AudioRepository
  lazy val seriesRepository = new SeriesRepository
  lazy val audioStorage = new AudioStorage

  lazy val ndlaClient = new NdlaClient
  lazy val migrationApiClient = new MigrationApiClient
  lazy val draftApiClient = new DraftApiClient

  lazy val importService = new ImportService
  lazy val readService = new ReadService
  lazy val writeService = new WriteService
  lazy val validationService = new ValidationService
  lazy val converterService = new ConverterService
  lazy val tagsService = new TagsService

  lazy val internController = new InternController
  lazy val resourcesApp = new ResourcesApp
  lazy val audioApiController = new AudioController
  lazy val seriesController = new SeriesController
  lazy val healthController = new HealthController

  lazy val e4sClient: NdlaE4sClient = Elastic4sClientFactory.getClient()
  lazy val searchConverterService = new SearchConverterService
  lazy val audioIndexService = new AudioIndexService
  lazy val audioSearchService = new AudioSearchService
  lazy val seriesIndexService = new SeriesIndexService
  lazy val seriesSearchService = new SeriesSearchService
  lazy val tagIndexService = new TagIndexService
  lazy val tagSearchService = new TagSearchService

  lazy val authRole = new AuthRole
  lazy val authUser = new AuthUser
  lazy val clock = new SystemClock

}
