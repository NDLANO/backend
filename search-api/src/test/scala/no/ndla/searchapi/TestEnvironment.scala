/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.Clock
import no.ndla.network.NdlaClient
import no.ndla.network.clients.{FeideApiClient, RedisClient}
import no.ndla.network.tapir.{NdlaMiddleware, Routes, Service}
import no.ndla.search.{BaseIndexService, Elastic4sClient}
import no.ndla.searchapi.controller.{InternController, SearchController}
import no.ndla.searchapi.integration._
import no.ndla.searchapi.model.api.ErrorHelpers
import no.ndla.searchapi.service.search._
import no.ndla.searchapi.service.ConverterService
import org.mockito.scalatest.MockitoSugar

trait TestEnvironment
    extends ArticleApiClient
    with MockitoSugar
    with ArticleIndexService
    with MultiSearchService
    with DraftIndexService
    with MultiDraftSearchService
    with ConverterService
    with DraftApiClient
    with FeideApiClient
    with RedisClient
    with Elastic4sClient
    with TaxonomyApiClient
    with IndexService
    with BaseIndexService
    with StrictLogging
    with LearningPathApiClient
    with NdlaClient
    with SearchConverterService
    with SearchService
    with SearchController
    with LearningPathIndexService
    with InternController
    with SearchApiClient
    with ErrorHelpers
    with Clock
    with GrepApiClient
    with Props
    with Routes[Eff]
    with NdlaMiddleware {
  override val props = new SearchApiProperties

  val searchController = mock[SearchController]
  val internController = mock[InternController]

  val ndlaClient               = mock[NdlaClient]
  var e4sClient: NdlaE4sClient = mock[NdlaE4sClient]

  val taxonomyApiClient = mock[TaxonomyApiClient]
  val grepApiClient     = mock[GrepApiClient]

  val draftApiClient        = mock[DraftApiClient]
  val learningPathApiClient = mock[LearningPathApiClient]
  val articleApiClient      = mock[ArticleApiClient]
  val feideApiClient        = mock[FeideApiClient]
  val redisClient           = mock[RedisClient]

  val clock = mock[SystemClock]

  val converterService         = mock[ConverterService]
  val searchConverterService   = mock[SearchConverterService]
  val multiSearchService       = mock[MultiSearchService]
  val articleIndexService      = mock[ArticleIndexService]
  val learningPathIndexService = mock[LearningPathIndexService]
  val draftIndexService        = mock[DraftIndexService]
  val multiDraftSearchService  = mock[MultiDraftSearchService]

  override val services: List[Service[Eff]] = List()
}
