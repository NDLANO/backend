/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import com.typesafe.scalalogging.StrictLogging
import com.zaxxer.hikari.HikariDataSource
import no.ndla.articleapi.auth.Role
import no.ndla.articleapi.caching.MemoizeHelpers
import no.ndla.articleapi.controller._
import no.ndla.articleapi.integration._
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service._
import no.ndla.articleapi.service.search._
import no.ndla.articleapi.validation.ContentValidator
import no.ndla.articleapi.integration.SearchApiClient
import no.ndla.articleapi.model.api.ErrorHelpers
import no.ndla.articleapi.model.domain.DBArticle
import no.ndla.common.Clock
import no.ndla.network.NdlaClient
import no.ndla.network.clients.{FeideApiClient, RedisClient}
import no.ndla.network.scalatra.{NdlaControllerBase, NdlaSwaggerSupport}
import no.ndla.search.{BaseIndexService, Elastic4sClient, NdlaE4sClient}
import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment
    extends Elastic4sClient
    with ArticleSearchService
    with ArticleIndexService
    with IndexService
    with BaseIndexService
    with SearchService
    with StrictLogging
    with NdlaController
    with NdlaControllerBase
    with NdlaSwaggerSupport
    with ArticleControllerV2
    with InternController
    with HealthController
    with DataSource
    with ArticleRepository
    with MockitoSugar
    with DraftApiClient
    with SearchApiClient
    with FeideApiClient
    with RedisClient
    with ConverterService
    with NdlaClient
    with SearchConverterService
    with ReadService
    with WriteService
    with ContentValidator
    with Clock
    with Role
    with ErrorHelpers
    with ArticleApiInfo
    with MemoizeHelpers
    with DBArticle
    with Props
    with TestData
    with DBMigrator {
  val props: ArticleApiProperties = new ArticleApiProperties
  val TestData: TestData          = new TestData
  val migrator: DBMigrator        = mock[DBMigrator]

  val articleSearchService: ArticleSearchService = mock[ArticleSearchService]
  val articleIndexService: ArticleIndexService   = mock[ArticleIndexService]

  val internController: InternController       = mock[InternController]
  val articleControllerV2: ArticleControllerV2 = mock[ArticleControllerV2]

  val healthController: HealthController = mock[HealthController]

  val dataSource: HikariDataSource         = mock[HikariDataSource]
  val articleRepository: ArticleRepository = mock[ArticleRepository]

  val converterService: ConverterService = mock[ConverterService]
  val readService: ReadService           = mock[ReadService]
  val writeService: WriteService         = mock[WriteService]
  val contentValidator: ContentValidator = mock[ContentValidator]

  val ndlaClient: NdlaClient                         = mock[NdlaClient]
  val searchConverterService: SearchConverterService = mock[SearchConverterService]
  var e4sClient: NdlaE4sClient                       = mock[NdlaE4sClient]
  val draftApiClient: DraftApiClient                 = mock[DraftApiClient]
  val searchApiClient: SearchApiClient               = mock[SearchApiClient]
  val feideApiClient: FeideApiClient                 = mock[FeideApiClient]
  val redisClient: RedisClient                       = mock[RedisClient]

  val clock: SystemClock = mock[SystemClock]
  val authRole           = new AuthRole
}
