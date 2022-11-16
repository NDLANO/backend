/*
 * Part of NDLA listing_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.configuration.BaseComponentRegistry
import no.ndla.common.scalatra.{NdlaControllerBase, NdlaSwaggerSupport}
import no.ndla.network.NdlaClient
import no.ndla.network.clients.FeideApiClient
import no.ndla.search.{BaseIndexService, Elastic4sClient, Elastic4sClientFactory, NdlaE4sClient}
import no.ndla.searchapi.auth.User
import no.ndla.searchapi.controller.{HealthController, InternController, NdlaController, SearchController}
import no.ndla.searchapi.integration._
import no.ndla.searchapi.model.api.ErrorHelpers
import no.ndla.searchapi.service.search._
import no.ndla.searchapi.service.{ApiSearchService, ConverterService, SearchClients}

class ComponentRegistry(properties: SearchApiProperties)
    extends BaseComponentRegistry[SearchApiProperties]
    with ArticleApiClient
    with ArticleIndexService
    with LearningPathIndexService
    with DraftIndexService
    with MultiSearchService
    with ErrorHelpers
    with MultiDraftSearchService
    with AudioApiClient
    with ConverterService
    with DraftApiClient
    with Elastic4sClient
    with HealthController
    with TaxonomyApiClient
    with ImageApiClient
    with IndexService
    with BaseIndexService
    with StrictLogging
    with LearningPathApiClient
    with NdlaClient
    with SearchClients
    with SearchConverterService
    with SearchService
    with ApiSearchService
    with SearchController
    with NdlaSwaggerSupport
    with FeideApiClient
    with InternController
    with User
    with SearchApiClient
    with GrepApiClient
    with Props
    with NdlaController
    with NdlaControllerBase
    with SearchApiInfo {
  override val props: SearchApiProperties = properties
  import props._

  implicit val swagger = new SearchSwagger

  lazy val searchController = new SearchController
  lazy val healthController = new HealthController
  lazy val internController = new InternController
  lazy val resourcesApp     = new ResourcesApp

  lazy val ndlaClient          = new NdlaClient
  var e4sClient: NdlaE4sClient = Elastic4sClientFactory.getClient(SearchServer)

  lazy val taxonomyApiClient = new TaxonomyApiClient
  lazy val grepApiClient     = new GrepApiClient

  lazy val draftApiClient        = new DraftApiClient(DraftApiUrl)
  lazy val learningPathApiClient = new LearningPathApiClient(LearningpathApiUrl)
  lazy val imageApiClient        = new ImageApiClient(ImageApiUrl)
  lazy val audioApiClient        = new AudioApiClient(AudioApiUrl)
  lazy val articleApiClient      = new ArticleApiClient(ArticleApiUrl)
  lazy val feideApiClient        = new FeideApiClient
  lazy val SearchClients = Map[String, SearchApiClient](
    draftApiClient.name        -> draftApiClient,
    learningPathApiClient.name -> learningPathApiClient,
    imageApiClient.name        -> imageApiClient,
    audioApiClient.name        -> audioApiClient
  )

  lazy val searchService            = new ApiSearchService
  lazy val converterService         = new ConverterService
  lazy val searchConverterService   = new SearchConverterService
  lazy val multiSearchService       = new MultiSearchService
  lazy val articleIndexService      = new ArticleIndexService
  lazy val learningPathIndexService = new LearningPathIndexService
  lazy val draftIndexService        = new DraftIndexService
  lazy val multiDraftSearchService  = new MultiDraftSearchService

  lazy val user = new User
}
