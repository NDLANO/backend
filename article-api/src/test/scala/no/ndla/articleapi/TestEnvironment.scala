/*
 * Part of NDLA article-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import com.zaxxer.hikari.HikariDataSource
import no.ndla.articleapi.caching.MemoizeHelpers
import no.ndla.articleapi.controller.*
import no.ndla.articleapi.integration.*
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service.*
import no.ndla.articleapi.service.search.*
import no.ndla.articleapi.validation.ContentValidator
import no.ndla.articleapi.integration.SearchApiClient
import no.ndla.articleapi.model.api.ErrorHandling
import no.ndla.articleapi.model.domain.DBArticle
import no.ndla.common.Clock
import no.ndla.database.{DBMigrator, DBUtility, DataSource}
import no.ndla.network.NdlaClient
import no.ndla.network.clients.{FeideApiClient, RedisClient}
import no.ndla.network.tapir.TapirApplication
import no.ndla.search.{BaseIndexService, Elastic4sClient, SearchLanguage}
import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment
    extends TapirApplication
    with Elastic4sClient
    with ArticleSearchService
    with ArticleIndexService
    with IndexService
    with BaseIndexService
    with SearchService
    with ArticleControllerV2
    with InternController
    with DataSource
    with ArticleRepository
    with MockitoSugar
    with SearchApiClient
    with FeideApiClient
    with RedisClient
    with ConverterService
    with NdlaClient
    with SearchConverterService
    with ReadService
    with WriteService
    with DBUtility
    with ContentValidator
    with Clock
    with ErrorHandling
    with MemoizeHelpers
    with DBArticle
    with Props
    with TestData
    with DBMigrator
    with SearchLanguage
    with FrontpageApiClient
    with ImageApiClient {
  val props: ArticleApiProperties = new ArticleApiProperties {
    override def InlineHtmlTags: Set[String]       = Set("code", "em", "span", "strong", "sub", "sup")
    override def IntroductionHtmlTags: Set[String] = InlineHtmlTags ++ Set("br", "p")
  }
  val TestData: TestData   = new TestData
  val migrator: DBMigrator = mock[DBMigrator]
  val DBUtil: DBUtility    = mock[DBUtility]

  val articleSearchService: ArticleSearchService = mock[ArticleSearchService]
  val articleIndexService: ArticleIndexService   = mock[ArticleIndexService]

  val internController: InternController       = mock[InternController]
  val articleControllerV2: ArticleControllerV2 = mock[ArticleControllerV2]

  val healthController: TapirHealthController = mock[TapirHealthController]

  val dataSource: HikariDataSource         = mock[HikariDataSource]
  val articleRepository: ArticleRepository = mock[ArticleRepository]

  val converterService: ConverterService = mock[ConverterService]
  val readService: ReadService           = mock[ReadService]
  val writeService: WriteService         = mock[WriteService]
  val contentValidator: ContentValidator = mock[ContentValidator]

  val ndlaClient: NdlaClient                         = mock[NdlaClient]
  val myndlaApiClient: MyNDLAApiClient               = mock[MyNDLAApiClient]
  val searchConverterService: SearchConverterService = mock[SearchConverterService]
  var e4sClient: NdlaE4sClient                       = mock[NdlaE4sClient]
  val searchApiClient: SearchApiClient               = mock[SearchApiClient]
  val feideApiClient: FeideApiClient                 = mock[FeideApiClient]
  val redisClient: RedisClient                       = mock[RedisClient]
  val frontpageApiClient: FrontpageApiClient         = mock[FrontpageApiClient]
  val imageApiClient: ImageApiClient                 = mock[ImageApiClient]

  val clock: SystemClock = mock[SystemClock]

  def services: List[TapirController] = List.empty
}
