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
  override val props: SearchApiProperties = properties
  import props._

  lazy val ndlaClient          = new NdlaClient
  var e4sClient: NdlaE4sClient = Elastic4sClientFactory.getClient(SearchServer)

  lazy val myndlaApiClient: MyNDLAApiClient = new MyNDLAApiClient

  lazy val taxonomyApiClient = new TaxonomyApiClient
  lazy val grepApiClient     = new GrepApiClient

  lazy val draftApiClient        = new DraftApiClient(DraftApiUrl)
  lazy val draftConceptApiClient = new DraftConceptApiClient(ConceptApiUrl)
  lazy val learningPathApiClient = new LearningPathApiClient(LearningpathApiUrl)
  lazy val articleApiClient      = new ArticleApiClient(ArticleApiUrl)
  lazy val feideApiClient        = new FeideApiClient
  lazy val redisClient           = new RedisClient(props.RedisHost, props.RedisPort)
  lazy val frontpageApiClient    = new FrontpageApiClient

  lazy val converterService         = new ConverterService
  lazy val searchConverterService   = new SearchConverterService
  lazy val multiSearchService       = new MultiSearchService
  lazy val articleIndexService      = new ArticleIndexService
  lazy val draftConceptIndexService = new DraftConceptIndexService
  lazy val learningPathIndexService = new LearningPathIndexService
  lazy val draftIndexService        = new DraftIndexService
  lazy val multiDraftSearchService  = new MultiDraftSearchService
  lazy val grepIndexService         = new GrepIndexService
  lazy val grepSearchService        = new GrepSearchService
  lazy val nodeIndexService         = new NodeIndexService

  lazy val searchController                        = new SearchController
  lazy val healthController: TapirHealthController = new TapirHealthController
  lazy val internController                        = new InternController
  lazy val clock: SystemClock                      = new SystemClock

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
