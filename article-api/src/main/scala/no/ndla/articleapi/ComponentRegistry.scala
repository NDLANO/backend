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

class ComponentRegistry(properties: ArticleApiProperties)
    extends BaseComponentRegistry[ArticleApiProperties]
    with TapirApplication
    with Props
    with DataSource
    with InternController
    with ArticleControllerV2
    with ArticleRepository
    with Elastic4sClient
    with SearchApiClient
    with FeideApiClient
    with RedisClient
    with ArticleSearchService
    with IndexService
    with BaseIndexService
    with SearchLanguage
    with ArticleIndexService
    with SearchService
    with ConverterService
    with NdlaClient
    with SearchConverterService
    with ReadService
    with MemoizeHelpers
    with WriteService
    with DBUtility
    with ContentValidator
    with Clock
    with ErrorHandling
    with DBArticle
    with DBMigrator
    with SwaggerDocControllerConfig
    with FrontpageApiClient
    with ImageApiClient
    with V55__SetHideBylineForImagesNotCopyrighted {
  override lazy val props: ArticleApiProperties = properties
  override lazy val migrator: DBMigrator        = DBMigrator(
    new R__SetArticleLanguageFromTaxonomy(props),
    new R__SetArticleTypeFromTaxonomy,
    new V8__CopyrightFormatUpdated,
    new V9__TranslateUntranslatedAuthors,
    new V20__UpdateH5PDomainForFF,
    new V22__UpdateH5PDomainForFFVisualElement,
    new V33__ConvertLanguageUnknown(props),
    new V55__SetHideBylineForImagesNotCopyrighted
  )
  override lazy val DBUtil: DBUtility = new DBUtility

  override lazy val dataSource: HikariDataSource            = DataSource.getHikariDataSource
  override lazy val internController                        = new InternController
  override lazy val articleControllerV2                     = new ArticleControllerV2
  override lazy val healthController: TapirHealthController = new TapirHealthController

  override lazy val articleRepository    = new ArticleRepository
  override lazy val articleSearchService = new ArticleSearchService
  override lazy val articleIndexService  = new ArticleIndexService

  override lazy val converterService = new ConverterService
  override lazy val contentValidator = new ContentValidator()

  override lazy val ndlaClient             = new NdlaClient
  override lazy val searchConverterService = new SearchConverterService
  override lazy val readService            = new ReadService
  override lazy val writeService           = new WriteService

  var e4sClient: NdlaE4sClient         = Elastic4sClientFactory.getClient(props.SearchServer)
  override lazy val searchApiClient    = new SearchApiClient
  override lazy val feideApiClient     = new FeideApiClient
  override lazy val myndlaApiClient    = new MyNDLAApiClient
  override lazy val redisClient        = new RedisClient(props.RedisHost, props.RedisPort)
  override lazy val frontpageApiClient = new FrontpageApiClient
  override lazy val imageApiClient     = new ImageApiClient

  lazy val clock = new SystemClock

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
