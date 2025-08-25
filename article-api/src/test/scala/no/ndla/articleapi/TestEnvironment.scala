/*
 * Part of NDLA article-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import no.ndla.articleapi.caching.MemoizeHelpers
import no.ndla.articleapi.controller.*
import no.ndla.articleapi.integration.*
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service.*
import no.ndla.articleapi.service.search.*
import no.ndla.articleapi.validation.ContentValidator
import no.ndla.articleapi.model.domain.DBArticle
import no.ndla.common.Clock
import no.ndla.database.{DBMigrator, DBUtility, DataSource}
import no.ndla.network.NdlaClient
import no.ndla.network.clients.{FeideApiClient, RedisClient, SearchApiClient}
import no.ndla.network.tapir.{ErrorHandling, ErrorHelpers, SwaggerController, TapirController, TapirHealthController}
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.search.{NdlaE4sClient, SearchLanguage}
import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment extends MockitoSugar {
  given props: ArticleApiProperties = new ArticleApiProperties {
    override def InlineHtmlTags: Set[String]       = Set("code", "em", "span", "strong", "sub", "sup")
    override def IntroductionHtmlTags: Set[String] = InlineHtmlTags ++ Set("br", "p")
  }

  lazy val TestData: TestData                      = new TestData
  implicit lazy val migrator: DBMigrator           = mock[DBMigrator]
  implicit lazy val dbUtility: DBUtility           = mock[DBUtility]
  implicit lazy val dbArticle: DBArticle           = mock[DBArticle]
  implicit lazy val memoizeHelpers: MemoizeHelpers = mock[MemoizeHelpers]
  implicit lazy val searchLanguage: SearchLanguage = mock[SearchLanguage]
  implicit lazy val errorHelpers: ErrorHelpers     = mock[ErrorHelpers]
  implicit lazy val errorHandling: ErrorHandling   = mock[ErrorHandling]

  implicit lazy val articleSearchService: ArticleSearchService = mock[ArticleSearchService]
  implicit lazy val articleIndexService: ArticleIndexService   = mock[ArticleIndexService]

  implicit lazy val internController: InternController       = mock[InternController]
  implicit lazy val articleControllerV2: ArticleControllerV2 = mock[ArticleControllerV2]

  implicit lazy val healthController: TapirHealthController = mock[TapirHealthController]

  implicit lazy val dataSource: DataSource               = mock[DataSource]
  implicit lazy val articleRepository: ArticleRepository = mock[ArticleRepository]

  implicit lazy val converterService: ConverterService = mock[ConverterService]
  implicit lazy val readService: ReadService           = mock[ReadService]
  implicit lazy val writeService: WriteService         = mock[WriteService]
  implicit lazy val contentValidator: ContentValidator = mock[ContentValidator]

  implicit lazy val ndlaClient: NdlaClient                         = mock[NdlaClient]
  implicit lazy val myndlaApiClient: MyNDLAApiClient               = mock[MyNDLAApiClient]
  implicit lazy val searchConverterService: SearchConverterService = mock[SearchConverterService]
  implicit lazy val e4sClient: NdlaE4sClient                       = mock[NdlaE4sClient]
  implicit lazy val searchApiClient: SearchApiClient               = mock[SearchApiClient]
  implicit lazy val feideApiClient: FeideApiClient                 = mock[FeideApiClient]
  implicit lazy val redisClient: RedisClient                       = mock[RedisClient]
  implicit lazy val frontpageApiClient: FrontpageApiClient         = mock[FrontpageApiClient]
  implicit lazy val imageApiClient: ImageApiClient                 = mock[ImageApiClient]

  implicit lazy val clock: Clock = mock[Clock]

  def services: List[TapirController] = List.empty
  val swagger: SwaggerController      = mock[SwaggerController]
}
