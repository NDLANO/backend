/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.converter.CommonConverter
import no.ndla.common.{Clock, UUIDUtil}
import no.ndla.database.{DBMigrator, DataSource}
import no.ndla.learningpathapi.controller.{InternController, LearningpathControllerV2, StatsController}
import no.ndla.learningpathapi.integration.*
import no.ndla.learningpathapi.model.api.ErrorHandling
import no.ndla.learningpathapi.repository.LearningPathRepository
import no.ndla.learningpathapi.service.*
import no.ndla.learningpathapi.service.search.{SearchConverterServiceComponent, SearchIndexService, SearchService}
import no.ndla.learningpathapi.validation.*
import no.ndla.network.NdlaClient
import no.ndla.network.clients.{FeideApiClient, MyNDLAApiClient, RedisClient}
import no.ndla.network.tapir.TapirApplication
import no.ndla.search.{BaseIndexService, Elastic4sClient, SearchLanguage}
import org.mockito.Mockito.reset
import org.scalatestplus.mockito.MockitoSugar
import no.ndla.database.DBUtility

trait TestEnvironment extends TapirApplication with MockitoSugar {
  given props = new LearningpathApiProperties

  given migrator: DBMigrator         = mock[DBMigrator]
  given dataSource: HikariDataSource = mock[HikariDataSource]

  given learningPathRepository: LearningPathRepository     = mock[LearningPathRepository]
  given readService: ReadService                           = mock[ReadService]
  given updateService: UpdateService                       = mock[UpdateService]
  given searchConverterService: SearchConverterService     = mock[SearchConverterService]
  given searchService: SearchService                       = mock[SearchService]
  given searchIndexService: SearchIndexService             = mock[SearchIndexService]
  given converterService: ConverterService                 = org.mockito.Mockito.spy(new ConverterService)
  given clock: SystemClock                                 = mock[SystemClock]
  given uuidUtil: UUIDUtil                                 = mock[UUIDUtil]
  given taxonomyApiClient: TaxonomyApiClient               = mock[TaxonomyApiClient]
  given ndlaClient: NdlaClient                             = mock[NdlaClient]
  given languageValidator: LanguageValidator               = mock[LanguageValidator]
  given learningpathControllerV2: LearningpathControllerV2 = mock[LearningpathControllerV2]
  given statsController: StatsController                   = mock[StatsController]
  given internController: InternController                 = mock[InternController]
  given healthController: TapirHealthController            = mock[TapirHealthController]
  given learningStepValidator: LearningStepValidator       = mock[LearningStepValidator]
  given learningPathValidator: LearningPathValidator       = mock[LearningPathValidator]
  given titleValidator: TitleValidator                     = mock[TitleValidator]
  var e4sClient: NdlaE4sClient                             = mock[NdlaE4sClient]
  given searchApiClient: SearchApiClient                   = mock[SearchApiClient]
  given oembedProxyClient: OembedProxyClient               = mock[OembedProxyClient]
  given feideApiClient: FeideApiClient                     = mock[FeideApiClient]
  given redisClient: RedisClient                           = mock[RedisClient]
  given myndlaApiClient: MyNDLAApiClient                   = mock[MyNDLAApiClient]
  given DBUtil: DBUtility                                  = mock[DBUtility]

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
