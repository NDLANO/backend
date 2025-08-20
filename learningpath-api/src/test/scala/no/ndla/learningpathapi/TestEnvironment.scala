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
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service.*
import no.ndla.learningpathapi.service.search.{SearchConverterServiceComponent, SearchIndexService, SearchService}
import no.ndla.learningpathapi.validation.*
import no.ndla.network.NdlaClient
import no.ndla.network.clients.{FeideApiClient, MyNDLAApiClient, RedisClient}
import no.ndla.network.tapir.TapirApplication
import no.ndla.search.{BaseIndexService, Elastic4sClient, SearchLanguage}
import org.mockito.Mockito.{reset, spy}
import org.scalatestplus.mockito.MockitoSugar
import no.ndla.database.DBUtility

trait TestEnvironment
    extends TapirApplication
    with LearningpathControllerV2
    with StatsController
    with LearningPathRepositoryComponent
    with FeideApiClient
    with ReadService
    with UpdateService
    with DBUtility
    with SearchConverterServiceComponent
    with SearchService
    with SearchLanguage
    with SearchIndexService
    with BaseIndexService
    with SearchApiClient
    with TaxonomyApiClient
    with NdlaClient
    with CommonConverter
    with ConverterService
    with OembedProxyClient
    with Elastic4sClient
    with DataSource
    with MockitoSugar
    with Clock
    with UUIDUtil
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

  override lazy val migrator: DBMigrator         = mock[DBMigrator]
  override lazy val dataSource: HikariDataSource = mock[HikariDataSource]

  override lazy val learningPathRepository: LearningPathRepository     = mock[LearningPathRepository]
  override lazy val readService: ReadService                           = mock[ReadService]
  override lazy val updateService: UpdateService                       = mock[UpdateService]
  override lazy val searchConverterService: SearchConverterService     = mock[SearchConverterService]
  override lazy val searchService: SearchService                       = mock[SearchService]
  override lazy val searchIndexService: SearchIndexService             = mock[SearchIndexService]
  override lazy val converterService: ConverterService                 = spy(new ConverterService)
  override lazy val clock: SystemClock                                 = mock[SystemClock]
  override lazy val uuidUtil: UUIDUtil                                 = mock[UUIDUtil]
  override lazy val taxonomyApiClient: TaxonomyApiClient               = mock[TaxonomyApiClient]
  override lazy val ndlaClient: NdlaClient                             = mock[NdlaClient]
  override lazy val languageValidator: LanguageValidator               = spy(new LanguageValidator)
  override lazy val learningpathControllerV2: LearningpathControllerV2 = mock[LearningpathControllerV2]
  override lazy val statsController: StatsController                   = mock[StatsController]
  override lazy val internController: InternController                 = mock[InternController]
  override lazy val healthController: TapirHealthController            = mock[TapirHealthController]
  override lazy val learningStepValidator: LearningStepValidator       = mock[LearningStepValidator]
  override lazy val learningPathValidator: LearningPathValidator       = mock[LearningPathValidator]
  override lazy val titleValidator: TitleValidator                     = spy(new TitleValidator)
  var e4sClient: NdlaE4sClient                                         = mock[NdlaE4sClient]
  override lazy val searchApiClient: SearchApiClient                   = mock[SearchApiClient]
  override lazy val oembedProxyClient: OembedProxyClient               = mock[OembedProxyClient]
  override lazy val feideApiClient: FeideApiClient                     = mock[FeideApiClient]
  override lazy val redisClient: RedisClient                           = mock[RedisClient]
  override lazy val myndlaApiClient: MyNDLAApiClient                   = mock[MyNDLAApiClient]
  override lazy val DBUtil: DBUtility                                  = mock[DBUtility]

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
