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

class ComponentRegistry(properties: SearchApiProperties)
    extends BaseComponentRegistry[SearchApiProperties]
    with TapirApplication
    with ArticleApiClient
    with ArticleIndexService
    with DraftConceptApiClient
    with DraftConceptIndexService
    with LearningPathIndexService
    with DraftIndexService
    with NodeIndexService
    with FrontpageApiClient
    with MultiSearchService
    with ErrorHandling
    with Clock
    with MultiDraftSearchService
    with ConverterService
    with DraftApiClient
    with Elastic4sClient
    with TaxonomyApiClient
    with IndexService
    with BaseIndexService
    with SearchLanguage
    with StrictLogging
    with LearningPathApiClient
    with NdlaClient
    with SearchConverterService
    with MyNDLAApiClient
    with SearchService
    with SearchController
    with GetSearchQueryParams
    with FeideApiClient
    with RedisClient
    with InternController
    with GrepIndexService
    with SearchApiClient
    with GrepApiClient
    with Props
    with SwaggerDocControllerConfig
    with GrepSearchService {
  override lazy val props: SearchApiProperties = properties

  override lazy val ndlaClient = new NdlaClient
  var e4sClient: NdlaE4sClient = Elastic4sClientFactory.getClient(props.SearchServer)

  override lazy val myndlaApiClient: MyNDLAApiClient = new MyNDLAApiClient

  override lazy val taxonomyApiClient = new TaxonomyApiClient
  override lazy val grepApiClient     = new GrepApiClient

  override lazy val draftApiClient        = new DraftApiClient(props.DraftApiUrl)
  override lazy val draftConceptApiClient = new DraftConceptApiClient(props.ConceptApiUrl)
  override lazy val learningPathApiClient = new LearningPathApiClient(props.LearningpathApiUrl)
  override lazy val articleApiClient      = new ArticleApiClient(props.ArticleApiUrl)
  override lazy val feideApiClient        = new FeideApiClient
  override lazy val redisClient           = new RedisClient(props.RedisHost, props.RedisPort)
  override lazy val frontpageApiClient    = new FrontpageApiClient

  override lazy val converterService         = new ConverterService
  override lazy val searchConverterService   = new SearchConverterService
  override lazy val multiSearchService       = new MultiSearchService
  override lazy val articleIndexService      = new ArticleIndexService
  override lazy val draftConceptIndexService = new DraftConceptIndexService
  override lazy val learningPathIndexService = new LearningPathIndexService
  override lazy val draftIndexService        = new DraftIndexService
  override lazy val multiDraftSearchService  = new MultiDraftSearchService
  override lazy val grepIndexService         = new GrepIndexService
  override lazy val grepSearchService        = new GrepSearchService
  override lazy val nodeIndexService         = new NodeIndexService

  override lazy val searchController                        = new SearchController
  override lazy val healthController: TapirHealthController = new TapirHealthController
  override lazy val internController                        = new InternController
  override lazy val clock: SystemClock                      = new SystemClock

  val swagger = new SwaggerController(
    List[TapirController](
      searchController,
      internController,
      healthController
    ),
    SwaggerDocControllerConfig.swaggerInfo
  )

  override def services: List[TapirController] = swagger.getServices()
}
