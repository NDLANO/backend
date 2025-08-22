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

trait TestEnvironment extends TapirApplication with MockitoSugar {
  given props: ArticleApiProperties = new ArticleApiProperties {
    override def InlineHtmlTags: Set[String]       = Set("code", "em", "span", "strong", "sub", "sup")
    override def IntroductionHtmlTags: Set[String] = InlineHtmlTags ++ Set("br", "p")
  }

  lazy val TestData: TestDataClass = new TestData
  given migrator: DBMigrator       = mock[DBMigrator]
  given DBUtil: DBUtility          = mock[DBUtility]

  given articleSearchService: ArticleSearchService = mock[ArticleSearchService]
  given articleIndexService: ArticleIndexService   = mock[ArticleIndexService]

  given internController: InternController       = mock[InternController]
  given articleControllerV2: ArticleControllerV2 = mock[ArticleControllerV2]

  given healthController: TapirHealthController = mock[TapirHealthController]

  given dataSource: HikariDataSource         = mock[HikariDataSource]
  given articleRepository: ArticleRepository = mock[ArticleRepository]

  given converterService: ConverterService = mock[ConverterService]
  given readService: ReadService           = mock[ReadService]
  given writeService: WriteService         = mock[WriteService]
  given contentValidator: ContentValidator = mock[ContentValidator]

  given ndlaClient: NdlaClient                         = mock[NdlaClient]
  given myndlaApiClient: MyNDLAApiClient               = mock[MyNDLAApiClient]
  given searchConverterService: SearchConverterService = mock[SearchConverterService]
  var e4sClient: NdlaE4sClient                         = mock[NdlaE4sClient]
  given searchApiClient: SearchApiClient               = mock[SearchApiClient]
  given feideApiClient: FeideApiClient                 = mock[FeideApiClient]
  given redisClient: RedisClient                       = mock[RedisClient]
  given frontpageApiClient: FrontpageApiClient         = mock[FrontpageApiClient]
  given imageApiClient: ImageApiClient                 = mock[ImageApiClient]

  given clock: SystemClock = mock[SystemClock]

  def services: List[TapirController] = List.empty
  val swagger: SwaggerController      = mock[SwaggerController]
}
