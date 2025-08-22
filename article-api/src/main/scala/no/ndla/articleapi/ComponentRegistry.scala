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
import no.ndla.articleapi.controller.{ArticleControllerV2, InternController, SwaggerDocControllerConfig}
import no.ndla.articleapi.db.migrationwithdependencies.{
  R__SetArticleLanguageFromTaxonomy,
  R__SetArticleTypeFromTaxonomy,
  V20__UpdateH5PDomainForFF,
  V22__UpdateH5PDomainForFFVisualElement,
  V33__ConvertLanguageUnknown,
  V55__SetHideBylineForImagesNotCopyrighted,
  V8__CopyrightFormatUpdated,
  V9__TranslateUntranslatedAuthors
}
import no.ndla.articleapi.integration.*
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service.*
import no.ndla.articleapi.service.search.*
import no.ndla.articleapi.validation.ContentValidator
import no.ndla.articleapi.model.api.ErrorHandling
import no.ndla.articleapi.model.domain.DBArticle
import no.ndla.common.Clock
import no.ndla.common.configuration.BaseComponentRegistry
import no.ndla.database.{DBMigrator, DBUtility, DataSource}
import no.ndla.network.NdlaClient
import no.ndla.network.tapir.TapirApplication
import no.ndla.network.clients.{FeideApiClient, RedisClient, SearchApiClient}
import no.ndla.search.{BaseIndexService, Elastic4sClient, SearchLanguage}

class ComponentRegistry(properties: ArticleApiProperties) extends TapirApplication[ArticleApiProperties] {
  override given props: ArticleApiProperties = properties
  override given migrator: DBMigrator        = DBMigrator(
    new R__SetArticleLanguageFromTaxonomy(props),
    new R__SetArticleTypeFromTaxonomy,
    new V8__CopyrightFormatUpdated,
    new V9__TranslateUntranslatedAuthors,
    new V20__UpdateH5PDomainForFF,
    new V22__UpdateH5PDomainForFFVisualElement,
    new V33__ConvertLanguageUnknown(props),
    new V55__SetHideBylineForImagesNotCopyrighted
  )

  given ErrorHandling            = new ErrorHandling
  given DBUtil: DBUtility        = new DBUtility
  given dataSource: DataSource   = DataSource.getDataSource
  given clock: SystemClock       = new SystemClock
  given e4sClient: NdlaE4sClient = Elastic4sClientFactory.getClient(props.SearchServer)

  // Infrastructure clients
  given ndlaClient: NdlaClient                 = new NdlaClient
  given searchApiClient: SearchApiClient       = new SearchApiClient
  given feideApiClient: FeideApiClient         = new FeideApiClient
  given myndlaApiClient: MyNDLAApiClient       = new MyNDLAApiClient
  given redisClient: RedisClient               = new RedisClient(props.RedisHost, props.RedisPort)
  given frontpageApiClient: FrontpageApiClient = new FrontpageApiClient
  given imageApiClient: ImageApiClient         = new ImageApiClient

  // Repository
  given articleRepository: ArticleRepository = new ArticleRepository

  // Services
  given converterService: ConverterService             = new ConverterService
  given contentValidator: ContentValidator             = new ContentValidator()
  given searchConverterService: SearchConverterService = new SearchConverterService
  given readService: ReadService                       = new ReadService
  given writeService: WriteService                     = new WriteService

  // Search services
  given articleSearchService: ArticleSearchService = new ArticleSearchService
  given articleIndexService: ArticleIndexService   = new ArticleIndexService

  // Error handling
  given errorHandling: ErrorHandling = new ErrorHandling

  // Controllers
  given internController: InternController               = new InternController
  given articleControllerV2: ArticleControllerV2         = new ArticleControllerV2
  override given healthController: TapirHealthController = new TapirHealthController

  val swagger = new SwaggerController(
    List(
      articleControllerV2,
      internController,
      healthController
    ),
    SwaggerDocControllerConfig.swaggerInfo
  )

  override def services: List[TapirController] = swagger.getServices()
}
