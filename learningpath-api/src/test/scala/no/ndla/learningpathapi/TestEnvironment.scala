/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.Clock
import no.ndla.database.{DBMigrator, DataSource}
import no.ndla.learningpathapi.controller.{InternController, LearningpathControllerV2, StatsController}
import no.ndla.learningpathapi.integration.*
import no.ndla.learningpathapi.model.api.ErrorHandling
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service.*
import no.ndla.learningpathapi.service.search.{SearchConverterServiceComponent, SearchIndexService, SearchService}
import no.ndla.learningpathapi.validation.*
import no.ndla.network.NdlaClient
import no.ndla.network.clients.{FeideApiClient, MyNDLAApiClient, RedisClient}
import no.ndla.network.tapir.TapirApplication
import no.ndla.search.{BaseIndexService, Elastic4sClient, SearchLanguage}
import org.mockito.Mockito.reset
import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment
    extends TapirApplication
    with LearningpathControllerV2
    with StatsController
    with LearningPathRepositoryComponent
    with FeideApiClient
    with ReadService
    with UpdateService
    with SearchConverterServiceComponent
    with SearchService
    with SearchLanguage
    with SearchIndexService
    with BaseIndexService
    with SearchApiClient
    with TaxonomyApiClient
    with NdlaClient
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
    with ErrorHandling
    with Props
    with InternController
    with DBMigrator
    with RedisClient {
  lazy val props = new LearningpathApiProperties

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

  def services: List[TapirController] = List.empty
  val swagger: SwaggerController      = mock[SwaggerController]

  def resetMocks(): Unit = {
    reset(dataSource)
    reset(learningPathRepository)
    reset(readService)
    reset(updateService)
    reset(searchService)
    reset(searchIndexService)
    reset(converterService)
    reset(searchConverterService)
    reset(languageValidator)
    reset(titleValidator)
    reset(e4sClient)
    reset(oembedProxyClient)
    reset(feideApiClient)
    reset(myndlaApiClient)
  }
}
