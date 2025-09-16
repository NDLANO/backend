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
import no.ndla.articleapi.db.migrationwithdependencies.{
  R__SetArticleLanguageFromTaxonomy,
  R__SetArticleTypeFromTaxonomy,
  V20__UpdateH5PDomainForFF,
  V22__UpdateH5PDomainForFFVisualElement,
  V33__ConvertLanguageUnknown,
  V55__SetHideBylineForImagesNotCopyrighted,
  V62__ComputeSearchTraits,
  V64__SetResourceTypeFromTaxonomyAsTag,
  V8__CopyrightFormatUpdated,
  V9__TranslateUntranslatedAuthors
}
import no.ndla.articleapi.integration.*
import no.ndla.articleapi.model.domain.DBArticle
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service.*
import no.ndla.articleapi.service.search.*
import no.ndla.articleapi.validation.ContentValidator
import no.ndla.common.Clock
import no.ndla.common.util.TraitUtil
import no.ndla.database.{DBMigrator, DBUtility, DataSource}
import no.ndla.network.NdlaClient
import no.ndla.network.tapir.{
  ErrorHandling,
  ErrorHelpers,
  Routes,
  SwaggerController,
  TapirApplication,
  TapirController,
  TapirHealthController
}
import no.ndla.network.clients.{FeideApiClient, MyNDLAApiClient, RedisClient, SearchApiClient, TaxonomyApiClient}
import no.ndla.search.{Elastic4sClientFactory, NdlaE4sClient, SearchLanguage}

class ComponentRegistry(properties: ArticleApiProperties) extends TapirApplication[ArticleApiProperties] {
  given props: ArticleApiProperties  = properties
  given dataSource: DataSource       = DataSource.getDataSource
  given clock: Clock                 = new Clock
  given errorHelpers: ErrorHelpers   = new ErrorHelpers
  given errorHandling: ErrorHandling = new ControllerErrorHandling

  given e4sClient: NdlaE4sClient                       = Elastic4sClientFactory.getClient(props.SearchServer)
  given searchLanguage: SearchLanguage                 = new SearchLanguage
  given dbUtility: DBUtility                           = new DBUtility
  given dbArticle: DBArticle                           = new DBArticle
  given traitUtil: TraitUtil                           = new TraitUtil
  given articleRepository: ArticleRepository           = new ArticleRepository
  given converterService: ConverterService             = new ConverterService
  given redisClient: RedisClient                       = new RedisClient(props.RedisHost, props.RedisPort)
  given memoizeHelpers: MemoizeHelpers                 = new MemoizeHelpers
  given ndlaClient: NdlaClient                         = new NdlaClient
  given searchApiClient: SearchApiClient               = new SearchApiClient(props.SearchApiUrl)
  given feideApiClient: FeideApiClient                 = new FeideApiClient
  given myndlaApiClient: MyNDLAApiClient               = new MyNDLAApiClient
  given frontpageApiClient: FrontpageApiClient         = new FrontpageApiClient
  given imageApiClient: ImageApiClient                 = new ImageApiClient
  given taxonomyApiClient: TaxonomyApiClient           = new TaxonomyApiClient(props.TaxonomyUrl)
  given contentValidator: ContentValidator             = new ContentValidator()
  given searchConverterService: SearchConverterService = new SearchConverterService
  given articleIndexService: ArticleIndexService       = new ArticleIndexService
  given articleSearchService: ArticleSearchService     = new ArticleSearchService
  given readService: ReadService                       = new ReadService
  given writeService: WriteService                     = new WriteService
  given internController: InternController             = new InternController
  given articleControllerV2: ArticleControllerV2       = new ArticleControllerV2
  given healthController: TapirHealthController        = new TapirHealthController

  given migrator: DBMigrator = DBMigrator(
    new R__SetArticleLanguageFromTaxonomy(props),
    new R__SetArticleTypeFromTaxonomy,
    new V8__CopyrightFormatUpdated,
    new V9__TranslateUntranslatedAuthors,
    new V20__UpdateH5PDomainForFF,
    new V22__UpdateH5PDomainForFFVisualElement,
    new V33__ConvertLanguageUnknown(props),
    new V55__SetHideBylineForImagesNotCopyrighted(props),
    new V62__ComputeSearchTraits,
    new V64__SetResourceTypeFromTaxonomyAsTag
  )

  given swagger: SwaggerController = new SwaggerController(
    List[TapirController](
      articleControllerV2,
      internController,
      healthController
    ),
    SwaggerDocControllerConfig.swaggerInfo
  )

  given services: List[TapirController] = swagger.getServices()
  given routes: Routes                  = new Routes
}
