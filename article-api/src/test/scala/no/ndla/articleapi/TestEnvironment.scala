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
import no.ndla.articleapi.model.api.ErrorHandling
import no.ndla.articleapi.model.domain.DBArticle
import no.ndla.common.Clock
import no.ndla.database.{DBMigrator, DBUtility, DataSource}
import no.ndla.network.NdlaClient
import no.ndla.network.clients.{FeideApiClient, RedisClient, SearchApiClient}
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
    with TestDataT
    with DBMigrator
    with SearchLanguage
    with FrontpageApiClient
    with ImageApiClient {
  override lazy val props: ArticleApiProperties = new ArticleApiProperties {
    override def InlineHtmlTags: Set[String]       = Set("code", "em", "span", "strong", "sub", "sup")
    override def IntroductionHtmlTags: Set[String] = InlineHtmlTags ++ Set("br", "p")
  }

  lazy val TestData: TestDataClass       = new TestDataClass
  override lazy val migrator: DBMigrator = mock[DBMigrator]
  override lazy val DBUtil: DBUtility    = mock[DBUtility]

  override lazy val articleSearchService: ArticleSearchService = mock[ArticleSearchService]
  override lazy val articleIndexService: ArticleIndexService   = mock[ArticleIndexService]

  override lazy val internController: InternController       = mock[InternController]
  override lazy val articleControllerV2: ArticleControllerV2 = mock[ArticleControllerV2]

  override lazy val healthController: TapirHealthController = mock[TapirHealthController]

  override lazy val dataSource: HikariDataSource         = mock[HikariDataSource]
  override lazy val articleRepository: ArticleRepository = mock[ArticleRepository]

  override lazy val converterService: ConverterService = mock[ConverterService]
  override lazy val readService: ReadService           = mock[ReadService]
  override lazy val writeService: WriteService         = mock[WriteService]
  override lazy val contentValidator: ContentValidator = mock[ContentValidator]

  override lazy val ndlaClient: NdlaClient                         = mock[NdlaClient]
  override lazy val myndlaApiClient: MyNDLAApiClient               = mock[MyNDLAApiClient]
  override lazy val searchConverterService: SearchConverterService = mock[SearchConverterService]
  var e4sClient: NdlaE4sClient                                     = mock[NdlaE4sClient]
  override lazy val searchApiClient: SearchApiClient               = mock[SearchApiClient]
  override lazy val feideApiClient: FeideApiClient                 = mock[FeideApiClient]
  override lazy val redisClient: RedisClient                       = mock[RedisClient]
  override lazy val frontpageApiClient: FrontpageApiClient         = mock[FrontpageApiClient]
  override lazy val imageApiClient: ImageApiClient                 = mock[ImageApiClient]

  override lazy val clock: SystemClock = mock[SystemClock]

  def services: List[TapirController] = List.empty
  val swagger: SwaggerController      = mock[SwaggerController]
}
