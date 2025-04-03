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
import no.ndla.database.DBUtility
import no.ndla.network.NdlaClient
import no.ndla.network.clients.{FeideApiClient, FrontpageApiClient, MyNDLAApiClient, RedisClient}
import no.ndla.network.tapir.TapirApplication
import no.ndla.search.{BaseIndexService, Elastic4sClient, SearchLanguage}
import no.ndla.searchapi.controller.parameters.GetSearchQueryParams
import no.ndla.searchapi.controller.{InternController, SearchController}
import no.ndla.searchapi.integration.*
import no.ndla.searchapi.model.api.ErrorHandling
import no.ndla.searchapi.service.search.*
import no.ndla.searchapi.service.ConverterService
import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment
    extends TapirApplication
    with ArticleApiClient
    with MockitoSugar
    with ArticleIndexService
    with MultiSearchService
    with DraftIndexService
    with NodeIndexService
    with FrontpageApiClient
    with DraftConceptApiClient
    with DraftConceptIndexService
    with MultiDraftSearchService
    with ConverterService
    with DraftApiClient
    with FeideApiClient
    with RedisClient
    with Elastic4sClient
    with TaxonomyApiClient
    with DBUtility
    with IndexService
    with SearchLanguage
    with BaseIndexService
    with StrictLogging
    with LearningPathApiClient
    with NdlaClient
    with SearchConverterService
    with MyNDLAApiClient
    with SearchService
    with SearchController
    with GetSearchQueryParams
    with GrepSearchService
    with LearningPathIndexService
    with InternController
    with GrepIndexService
    with SearchApiClient
    with ErrorHandling
    with Clock
    with GrepApiClient
    with Props {
  override lazy val props = new SearchApiProperties

  val searchController: SearchController = mock[SearchController]
  val internController: InternController = mock[InternController]

  val ndlaClient: NdlaClient   = mock[NdlaClient]
  var e4sClient: NdlaE4sClient = mock[NdlaE4sClient]

  val myndlaApiClient: MyNDLAApiClient = mock[MyNDLAApiClient]

  val taxonomyApiClient: TaxonomyApiClient = mock[TaxonomyApiClient]
  val grepApiClient: GrepApiClient         = mock[GrepApiClient]

  val draftApiClient: DraftApiClient               = mock[DraftApiClient]
  val learningPathApiClient: LearningPathApiClient = mock[LearningPathApiClient]
  val articleApiClient: ArticleApiClient           = mock[ArticleApiClient]
  val draftConceptApiClient: DraftConceptApiClient = mock[DraftConceptApiClient]
  val feideApiClient: FeideApiClient               = mock[FeideApiClient]
  val redisClient: RedisClient                     = mock[RedisClient]
  val frontpageApiClient: FrontpageApiClient       = mock[FrontpageApiClient]
  val DBUtil: DBUtility                            = mock[DBUtility]

  val clock: SystemClock = mock[SystemClock]

  val converterService: ConverterService             = mock[ConverterService]
  val searchConverterService: SearchConverterService = mock[SearchConverterService]
  val multiSearchService: MultiSearchService         = mock[MultiSearchService]
  val grepSearchService: GrepSearchService           = mock[GrepSearchService]

  val articleIndexService: ArticleIndexService           = mock[ArticleIndexService]
  val learningPathIndexService: LearningPathIndexService = mock[LearningPathIndexService]
  val draftIndexService: DraftIndexService               = mock[DraftIndexService]
  val draftConceptIndexService: DraftConceptIndexService = mock[DraftConceptIndexService]
  val grepIndexService: GrepIndexService                 = mock[GrepIndexService]
  val nodeIndexService: NodeIndexService                 = mock[NodeIndexService]

  val multiDraftSearchService: MultiDraftSearchService = mock[MultiDraftSearchService]

  override def services: List[TapirController] = List()
  val swagger: SwaggerController               = mock[SwaggerController]
}
