/*
 * Part of NDLA search-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.Clock
import no.ndla.common.configuration.BaseComponentRegistry
import no.ndla.network.NdlaClient
import no.ndla.network.clients.{FeideApiClient, FrontpageApiClient, MyNDLAApiClient, RedisClient}
import no.ndla.network.tapir.TapirApplication
import no.ndla.search.{BaseIndexService, Elastic4sClient, SearchLanguage}
import no.ndla.searchapi.controller.parameters.GetSearchQueryParams
import no.ndla.searchapi.controller.{InternController, SearchController, SwaggerDocControllerConfig}
import no.ndla.searchapi.integration.*
import no.ndla.searchapi.model.api.ErrorHandling
import no.ndla.searchapi.service.search.*
import no.ndla.searchapi.service.ConverterService

class ComponentRegistry(properties: SearchApiProperties) extends TapirApplication[SearchApiProperties] {
  given props: SearchApiProperties = properties

  given ndlaClient               = new NdlaClient
  given e4sClient: NdlaE4sClient = Elastic4sClientFactory.getClient(props.SearchServer)

  given myndlaApiClient: MyNDLAApiClient = new MyNDLAApiClient

  given taxonomyApiClient     = new TaxonomyApiClient
  given grepApiClient         = new GrepApiClient
  given draftApiClient        = new DraftApiClient(props.DraftApiUrl)
  given draftConceptApiClient = new DraftConceptApiClient(props.ConceptApiUrl)
  given learningPathApiClient = new LearningPathApiClient(props.LearningpathApiUrl)
  given articleApiClient      = new ArticleApiClient(props.ArticleApiUrl)
  given feideApiClient        = new FeideApiClient
  given redisClient           = new RedisClient(props.RedisHost, props.RedisPort)
  given frontpageApiClient    = new FrontpageApiClient

  given converterService         = new ConverterService
  given searchConverterService   = new SearchConverterService
  given multiSearchService       = new MultiSearchService
  given articleIndexService      = new ArticleIndexService
  given draftConceptIndexService = new DraftConceptIndexService
  given learningPathIndexService = new LearningPathIndexService
  given draftIndexService        = new DraftIndexService
  given multiDraftSearchService  = new MultiDraftSearchService
  given grepIndexService         = new GrepIndexService
  given grepSearchService        = new GrepSearchService
  given nodeIndexService         = new NodeIndexService

  given searchController                        = new SearchController
  given healthController: TapirHealthController = new TapirHealthController
  given internController                        = new InternController
  given clock: SystemClock                      = new SystemClock

  val swagger = new SwaggerController(
    List[TapirController](
      searchController,
      internController,
      healthController
    ),
    SwaggerDocControllerConfig.swaggerInfo
  )

  def services: List[TapirController] = swagger.getServices()
  given routes                        = new Routes(services)
}
