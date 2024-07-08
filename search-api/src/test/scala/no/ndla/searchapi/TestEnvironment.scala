/*
 * Part of NDLA search-api
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
import no.ndla.network.tapir.{Routes, Service}
import no.ndla.search.{BaseIndexService, Elastic4sClient}
import no.ndla.searchapi.controller.{InternController, SearchController}
import no.ndla.searchapi.integration.*
import no.ndla.searchapi.model.api.ErrorHelpers
import no.ndla.searchapi.service.search.*
import no.ndla.searchapi.service.ConverterService
import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment
    extends ArticleApiClient
    with MockitoSugar
    with ArticleIndexService
    with MultiSearchService
    with DraftIndexService
    with DraftConceptApiClient
    with DraftConceptIndexService
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
    with MyNDLAApiClient
    with SearchService
    with SearchController
    with LearningPathIndexService
    with InternController
    with SearchApiClient
    with ErrorHelpers
    with Clock
    with GrepApiClient
    with Props
    with Routes[Eff] {
  override val props = new SearchApiProperties

  val searchController: SearchController = mock[SearchController]
  val internController: InternController = mock[InternController]

  val ndlaClient: NdlaClient   = mock[NdlaClient]
  var e4sClient: NdlaE4sClient = mock[NdlaE4sClient]

  val myndlaapiClient: MyNDLAApiClient = mock[MyNDLAApiClient]

  val taxonomyApiClient: TaxonomyApiClient = mock[TaxonomyApiClient]
  val grepApiClient: GrepApiClient         = mock[GrepApiClient]

  val draftApiClient: DraftApiClient               = mock[DraftApiClient]
  val learningPathApiClient: LearningPathApiClient = mock[LearningPathApiClient]
  val articleApiClient: ArticleApiClient           = mock[ArticleApiClient]
  val draftConceptApiClient: DraftConceptApiClient = mock[DraftConceptApiClient]
  val feideApiClient: FeideApiClient               = mock[FeideApiClient]
  val redisClient: RedisClient                     = mock[RedisClient]

  val clock: SystemClock = mock[SystemClock]

  val converterService: ConverterService             = mock[ConverterService]
  val searchConverterService: SearchConverterService = mock[SearchConverterService]
  val multiSearchService: MultiSearchService         = mock[MultiSearchService]

  val articleIndexService: ArticleIndexService           = mock[ArticleIndexService]
  val learningPathIndexService: LearningPathIndexService = mock[LearningPathIndexService]
  val draftIndexService: DraftIndexService               = mock[DraftIndexService]
  val draftConceptIndexService: DraftConceptIndexService = mock[DraftConceptIndexService]

  val multiDraftSearchService: MultiDraftSearchService = mock[MultiDraftSearchService]

  override def services: List[Service[Eff]] = List()
}
