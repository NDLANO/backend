/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.Clock
import no.ndla.learningpathapi.controller.{InternController, LearningpathControllerV2, StatsController}
import no.ndla.learningpathapi.integration._
import no.ndla.learningpathapi.model.api.ErrorHelpers
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service._
import no.ndla.learningpathapi.service.search.{SearchConverterServiceComponent, SearchIndexService, SearchService}
import no.ndla.learningpathapi.validation._
import no.ndla.network.NdlaClient
import no.ndla.network.clients.{FeideApiClient, RedisClient}
import no.ndla.network.tapir.{NdlaMiddleware, Routes, Service}
import no.ndla.search.{BaseIndexService, Elastic4sClient}
import org.mockito.scalatest.MockitoSugar

trait TestEnvironment
    extends LearningpathControllerV2
    with StatsController
    with LearningPathRepositoryComponent
    with FeideApiClient
    with ReadService
    with UpdateService
    with SearchConverterServiceComponent
    with SearchService
    with SearchIndexService
    with BaseIndexService
    with SearchApiClient
    with TaxonomyApiClient
    with NdlaClient
    with ImageApiClientComponent
    with ConverterService
    with OembedProxyClient
    with Elastic4sClient
    with DataSource
    with MockitoSugar
    with Clock
    with LanguageValidator
    with LearningPathValidator
    with LearningStepValidator
    with TitleValidator
    with TextValidator
    with UrlValidator
    with MyNDLAApiClient
    with ErrorHelpers
    with Props
    with InternController
    with DBMigrator
    with RedisClient
    with NdlaMiddleware
    with Routes[Eff] {
  val props = new LearningpathApiProperties

  val migrator: DBMigrator         = mock[DBMigrator]
  val dataSource: HikariDataSource = mock[HikariDataSource]

  val learningPathRepository: LearningPathRepository                   = mock[LearningPathRepository]
  val learningPathRepositoryComponent: LearningPathRepositoryComponent = mock[LearningPathRepositoryComponent]
  val readService: ReadService                                         = mock[ReadService]
  val updateService: UpdateService                                     = mock[UpdateService]
  val searchConverterService: SearchConverterService                   = mock[SearchConverterService]
  val searchService: SearchService                                     = mock[SearchService]
  val searchIndexService: SearchIndexService                           = mock[SearchIndexService]
  val converterService: ConverterService                               = org.mockito.Mockito.spy(new ConverterService)
  val clock: SystemClock                                               = mock[SystemClock]
  val taxonomyApiClient: TaxonomyApiClient                             = mock[TaxonomyApiClient]
  val ndlaClient: NdlaClient                                           = mock[NdlaClient]
  val imageApiClient: ImageApiClient                                   = mock[ImageApiClient]
  val languageValidator: LanguageValidator                             = mock[LanguageValidator]
  val learningpathControllerV2: LearningpathControllerV2               = mock[LearningpathControllerV2]
  val statsController: StatsController                                 = mock[StatsController]
  val internController: InternController                               = mock[InternController]
  val learningStepValidator: LearningStepValidator                     = mock[LearningStepValidator]
  val learningPathValidator: LearningPathValidator                     = mock[LearningPathValidator]
  val titleValidator: TitleValidator                                   = mock[TitleValidator]
  var e4sClient: NdlaE4sClient                                         = mock[NdlaE4sClient]
  val searchApiClient: SearchApiClient                                 = mock[SearchApiClient]
  val oembedProxyClient: OembedProxyClient                             = mock[OembedProxyClient]
  val feideApiClient: FeideApiClient                                   = mock[FeideApiClient]
  val redisClient: RedisClient                                         = mock[RedisClient]
  val myndlaApiClient: MyNDLAApiClient                                 = mock[MyNDLAApiClient]

  def services: List[Service[Eff]] = List.empty

  def resetMocks(): Unit = {
    reset(
      dataSource,
      learningPathRepository,
      readService,
      updateService,
      searchService,
      searchIndexService,
      converterService,
      searchConverterService,
      languageValidator,
      titleValidator,
      e4sClient,
      oembedProxyClient,
      feideApiClient,
      myndlaApiClient
    )
  }
}
