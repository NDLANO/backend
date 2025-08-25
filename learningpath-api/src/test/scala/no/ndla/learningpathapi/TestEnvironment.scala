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
import no.ndla.learningpathapi.repository.LearningPathRepository
import no.ndla.learningpathapi.service.*
import no.ndla.learningpathapi.service.search.{SearchConverterServiceComponent, SearchIndexService, SearchService}
import no.ndla.learningpathapi.validation.*
import no.ndla.network.NdlaClient
import no.ndla.network.clients.{FeideApiClient, MyNDLAApiClient, RedisClient}
import no.ndla.network.tapir.{
  ErrorHandling,
  ErrorHelpers,
  Routes,
  SwaggerController,
  TapirApplication,
  TapirController,
  TapirHealthController
}
import no.ndla.search.{BaseIndexService, NdlaE4sClient, SearchLanguage}
import org.mockito.Mockito.reset
import org.scalatestplus.mockito.MockitoSugar
import no.ndla.database.DBUtility

trait TestEnvironment extends TapirApplication[LearningpathApiProperties] with MockitoSugar {
  implicit lazy val props: LearningpathApiProperties = new LearningpathApiProperties

  implicit lazy val migrator: DBMigrator         = mock[DBMigrator]
  implicit lazy val dataSource: DataSource       = mock[DataSource]
  implicit lazy val errorHandling: ErrorHandling = mock[ErrorHandling]
  implicit lazy val errorHelpers: ErrorHelpers   = new ErrorHelpers
  implicit lazy val routes: Routes               = mock[Routes]

  implicit lazy val learningPathRepository: LearningPathRepository          = mock[LearningPathRepository]
  implicit lazy val readService: ReadService                                = mock[ReadService]
  implicit lazy val updateService: UpdateService                            = mock[UpdateService]
  implicit lazy val searchConverterService: SearchConverterServiceComponent = mock[SearchConverterServiceComponent]
  implicit lazy val searchService: SearchService                            = mock[SearchService]
  implicit lazy val searchIndexService: SearchIndexService                  = mock[SearchIndexService]
  implicit lazy val converterService: ConverterService                      = mock[ConverterService]
  implicit lazy val clock: Clock                                            = mock[Clock]
  implicit lazy val uuidUtil: UUIDUtil                                      = mock[UUIDUtil]
  implicit lazy val taxonomyApiClient: TaxonomyApiClient                    = mock[TaxonomyApiClient]
  implicit lazy val ndlaClient: NdlaClient                                  = mock[NdlaClient]
  implicit lazy val languageValidator: LanguageValidator                    = mock[LanguageValidator]
  implicit lazy val learningpathControllerV2: LearningpathControllerV2      = mock[LearningpathControllerV2]
  implicit lazy val statsController: StatsController                        = mock[StatsController]
  implicit lazy val internController: InternController                      = mock[InternController]
  implicit lazy val healthController: TapirHealthController                 = mock[TapirHealthController]
  implicit lazy val learningStepValidator: LearningStepValidator            = mock[LearningStepValidator]
  implicit lazy val urlValidator: UrlValidator                              = mock[UrlValidator]
  implicit lazy val learningPathValidator: LearningPathValidator            = mock[LearningPathValidator]
  implicit lazy val titleValidator: TitleValidator                          = mock[TitleValidator]
  implicit lazy val e4sClient: NdlaE4sClient                                = mock[NdlaE4sClient]
  implicit lazy val searchApiClient: SearchApiClient                        = mock[SearchApiClient]
  implicit lazy val oembedProxyClient: OembedProxyClient                    = mock[OembedProxyClient]
  implicit lazy val feideApiClient: FeideApiClient                          = mock[FeideApiClient]
  implicit lazy val redisClient: RedisClient                                = mock[RedisClient]
  implicit lazy val myndlaApiClient: MyNDLAApiClient                        = mock[MyNDLAApiClient]
  implicit lazy val DBUtil: DBUtility                                       = mock[DBUtility]
  implicit lazy val searchLanguage: SearchLanguage                          = mock[SearchLanguage]
  implicit lazy val commonConverter: CommonConverter                        = mock[CommonConverter]

  implicit lazy val services: List[TapirController] = List.empty
  implicit lazy val swagger: SwaggerController      = mock[SwaggerController]

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
