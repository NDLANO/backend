/*
 * Part of NDLA search-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import no.ndla.common.Clock
import no.ndla.common.util.TraitUtil
import no.ndla.network.NdlaClient
import no.ndla.network.clients.rediscache.FeideRedisClient
import no.ndla.network.clients.{FeideApiClient, FrontpageApiClient, MyNDLAApiClient, TaxonomyApiClient}
import no.ndla.network.tapir.{
  ErrorHelpers,
  Routes,
  SwaggerController,
  SwaggerInfo,
  TapirApplication,
  TapirController,
  TapirHealthController,
}
import no.ndla.search.{Elastic4sClientFactory, NdlaE4sClient, SearchLanguage}
import no.ndla.searchapi.controller.{ControllerErrorHandling, InternController, SearchController}
import no.ndla.searchapi.integration.*
import no.ndla.searchapi.service.search.*
import no.ndla.searchapi.service.ConverterService

class ComponentRegistry(properties: SearchApiProperties) extends TapirApplication[SearchApiProperties] {
  given props: SearchApiProperties                   = properties
  given ndlaClient: NdlaClient                       = new NdlaClient
  given clock: Clock                                 = new Clock
  given e4sClient: NdlaE4sClient                     = Elastic4sClientFactory.getClient(props.SearchServer)
  given searchLanguage: SearchLanguage               = new SearchLanguage
  given errorHelpers: ErrorHelpers                   = new ErrorHelpers
  given errorHandling: ControllerErrorHandling       = new ControllerErrorHandling
  given myndlaApiClient: MyNDLAApiClient             = new MyNDLAApiClient
  given taxonomyApiClient: TaxonomyApiClient         = new TaxonomyApiClient(props.TaxonomyUrl)
  given grepApiClient: GrepApiClient                 = new GrepApiClient
  given draftApiClient: DraftApiClient               = new DraftApiClient(props.DraftApiUrl)
  given draftConceptApiClient: DraftConceptApiClient = new DraftConceptApiClient(props.ConceptApiUrl)
  given learningPathApiClient: LearningPathApiClient = new LearningPathApiClient(props.LearningpathApiUrl)
  given articleApiClient: ArticleApiClient           = new ArticleApiClient(props.ArticleApiUrl)
  given redisClient: FeideRedisClient                = new FeideRedisClient(props.RedisHost, props.RedisPort)
  given feideApiClient: FeideApiClient               = new FeideApiClient
  given frontpageApiClient: FrontpageApiClient       = new FrontpageApiClient

  given converterService: ConverterService                 = new ConverterService
  given traitUtil: TraitUtil                               = new TraitUtil
  given searchConverterService: SearchConverterService     = new SearchConverterService
  given articleIndexService: ArticleIndexService           = new ArticleIndexService
  given learningPathIndexService: LearningPathIndexService = new LearningPathIndexService
  given draftIndexService: DraftIndexService               = new DraftIndexService
  given grepIndexService: GrepIndexService                 = new GrepIndexService
  given nodeIndexService: NodeIndexService                 = new NodeIndexService
  given multiSearchService: MultiSearchService             = new MultiSearchService
  given draftConceptIndexService: DraftConceptIndexService = new DraftConceptIndexService
  given multiDraftSearchService: MultiDraftSearchService   = new MultiDraftSearchService
  given grepSearchService: GrepSearchService               = new GrepSearchService

  given searchController: SearchController      = new SearchController
  given healthController: TapirHealthController = new TapirHealthController
  given internController: InternController      = new InternController

  given swaggerInfo: SwaggerInfo = SwaggerInfo(
    prefix = "search-api",
    description = "A common endpoint for searching across article, draft, learningpath, image and audio APIs.\n\n" +
      "The Search API provides a common endpoint for searching across the article, draft, learningpath, image and audio APIs. " +
      "The search does a free text search in data and metadata. It is also possible to search targeted at specific " +
      "meta-data fields like language or license.\n" +
      "Note that the query parameter is based on the Elasticsearch simple search language. For more information, see " +
      "https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-simple-query-string-query.html",
  )
  given swagger: SwaggerController = new SwaggerController(searchController, healthController, internController)

  given services: List[TapirController] = swagger.allServices
  given routes: Routes                  = new Routes
}
