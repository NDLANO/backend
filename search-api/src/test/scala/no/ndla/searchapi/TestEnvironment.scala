/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import com.typesafe.scalalogging.StrictLogging
import no.ndla.network.NdlaClient
import no.ndla.network.clients.{FeideApiClient, RedisClient}
import no.ndla.network.scalatra.{NdlaControllerBase, NdlaSwaggerSupport}
import no.ndla.search.{BaseIndexService, Elastic4sClient, NdlaE4sClient}
import no.ndla.searchapi.auth.User
import no.ndla.searchapi.controller.{HealthController, InternController, NdlaController, SearchController}
import no.ndla.searchapi.integration._
import no.ndla.searchapi.model.api.ErrorHelpers
import no.ndla.searchapi.service.search._
import no.ndla.searchapi.service.{ApiSearchService, ConverterService, SearchClients}
import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment
    extends ArticleApiClient
    with MockitoSugar
    with ArticleIndexService
    with MultiSearchService
    with DraftIndexService
    with MultiDraftSearchService
    with AudioApiClient
    with ConverterService
    with DraftApiClient
    with FeideApiClient
    with RedisClient
    with Elastic4sClient
    with HealthController
    with ImageApiClient
    with TaxonomyApiClient
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
    with User
    with LearningPathIndexService
    with InternController
    with SearchApiClient
    with NdlaController
    with NdlaControllerBase
    with ErrorHelpers
    with GrepApiClient
    with Props
    with SearchApiInfo {
  override val props = new SearchApiProperties

  val searchController: SearchController = mock[SearchController]
  val healthController: HealthController = mock[HealthController]
  val internController: InternController = mock[InternController]
  val resourcesApp: ResourcesApp     = mock[ResourcesApp]

  val ndlaClient: NdlaClient               = mock[NdlaClient]
  var e4sClient: NdlaE4sClient = mock[NdlaE4sClient]

  val taxonomyApiClient: TaxonomyApiClient = mock[TaxonomyApiClient]
  val grepApiClient: GrepApiClient     = mock[GrepApiClient]

  val draftApiClient: DraftApiClient        = mock[DraftApiClient]
  val learningPathApiClient: LearningPathApiClient = mock[LearningPathApiClient]
  val imageApiClient: ImageApiClient        = mock[ImageApiClient]
  val audioApiClient: AudioApiClient        = mock[AudioApiClient]
  val articleApiClient: ArticleApiClient      = mock[ArticleApiClient]
  val feideApiClient: FeideApiClient        = mock[FeideApiClient]
  val redisClient: RedisClient           = mock[RedisClient]

  val SearchClients: Map[String,SearchApiClient] = Map[String, SearchApiClient](
    "articles"      -> draftApiClient,
    "learningpaths" -> learningPathApiClient,
    "images"        -> imageApiClient,
    "audios"        -> audioApiClient
  )

  val searchService: ApiSearchService            = mock[ApiSearchService]
  val converterService: ConverterService         = mock[ConverterService]
  val searchConverterService: SearchConverterService   = mock[SearchConverterService]
  val multiSearchService: MultiSearchService       = mock[MultiSearchService]
  val articleIndexService: ArticleIndexService      = mock[ArticleIndexService]
  val learningPathIndexService: LearningPathIndexService = mock[LearningPathIndexService]
  val draftIndexService: DraftIndexService        = mock[DraftIndexService]
  val multiDraftSearchService: MultiDraftSearchService  = mock[MultiDraftSearchService]

  val user: User = mock[User]
}
