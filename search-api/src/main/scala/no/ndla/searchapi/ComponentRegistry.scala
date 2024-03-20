/*
 * Part of NDLA listing_api.
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
import no.ndla.network.clients.{FeideApiClient, RedisClient}
import no.ndla.network.tapir.{
  NdlaMiddleware,
  Routes,
  Service,
  SwaggerControllerConfig,
  TapirErrorHelpers,
  TapirHealthController
}
import no.ndla.search.{BaseIndexService, Elastic4sClient}
import no.ndla.searchapi.controller.{InternController, SearchController, SwaggerDocControllerConfig}
import no.ndla.searchapi.integration._
import no.ndla.searchapi.model.api.ErrorHelpers
import no.ndla.searchapi.service.search._
import no.ndla.searchapi.service.ConverterService

class ComponentRegistry(properties: SearchApiProperties)
    extends BaseComponentRegistry[SearchApiProperties]
    with ArticleApiClient
    with ArticleIndexService
    with LearningPathIndexService
    with DraftIndexService
    with MultiSearchService
    with ErrorHelpers
    with Clock
    with MultiDraftSearchService
    with ConverterService
    with DraftApiClient
    with Elastic4sClient
    with TaxonomyApiClient
    with IndexService
    with BaseIndexService
    with StrictLogging
    with LearningPathApiClient
    with NdlaClient
    with SearchConverterService
    with MyNDLAApiClient
    with SearchService
    with SearchController
    with FeideApiClient
    with RedisClient
    with InternController
    with SearchApiClient
    with GrepApiClient
    with Props
    with Routes[Eff]
    with NdlaMiddleware
    with TapirErrorHelpers
    with SwaggerControllerConfig
    with SwaggerDocControllerConfig
    with TapirHealthController {
  override val props: SearchApiProperties = properties
  import props._

  lazy val ndlaClient          = new NdlaClient
  var e4sClient: NdlaE4sClient = Elastic4sClientFactory.getClient(SearchServer)

  lazy val myndlaapiClient: MyNDLAApiClient = new MyNDLAApiClient

  lazy val taxonomyApiClient = new TaxonomyApiClient
  lazy val grepApiClient     = new GrepApiClient

  lazy val draftApiClient        = new DraftApiClient(DraftApiUrl)
  lazy val learningPathApiClient = new LearningPathApiClient(LearningpathApiUrl)
  lazy val articleApiClient      = new ArticleApiClient(ArticleApiUrl)
  lazy val feideApiClient        = new FeideApiClient
  lazy val redisClient           = new RedisClient(props.RedisHost, props.RedisPort)

  lazy val converterService         = new ConverterService
  lazy val searchConverterService   = new SearchConverterService
  lazy val multiSearchService       = new MultiSearchService
  lazy val articleIndexService      = new ArticleIndexService
  lazy val learningPathIndexService = new LearningPathIndexService
  lazy val draftIndexService        = new DraftIndexService
  lazy val multiDraftSearchService  = new MultiDraftSearchService

  lazy val searchController                             = new SearchController
  lazy val healthController: TapirHealthController[Eff] = new TapirHealthController[Eff]
  lazy val internController                             = new InternController
  lazy val clock: SystemClock                           = new SystemClock

  private val swagger = new SwaggerController(
    List[Service[Eff]](
      searchController,
      internController,
      healthController
    ),
    SwaggerDocControllerConfig.swaggerInfo
  )

  override def services: List[Service[Eff]] = swagger.getServices()
}
