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
import no.ndla.common.configuration.BaseProps
import no.ndla.database.{DBUtility, DatabaseProps, HasDatabaseProps}
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

class TestProps extends SearchApiProperties with BaseProps with DatabaseProps {
  override def MetaMigrationLocation: String = ???
}

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
    with HasDatabaseProps
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
  override lazy val props: TestProps = new TestProps

  override lazy val searchController: SearchController      = mock[SearchController]
  override lazy val internController: InternController      = mock[InternController]
  override lazy val healthController: TapirHealthController = mock[TapirHealthController]

  override lazy val ndlaClient: NdlaClient = mock[NdlaClient]
  var e4sClient: NdlaE4sClient             = mock[NdlaE4sClient]

  override lazy val myndlaApiClient: MyNDLAApiClient = mock[MyNDLAApiClient]

  override lazy val taxonomyApiClient: TaxonomyApiClient = mock[TaxonomyApiClient]
  override lazy val grepApiClient: GrepApiClient         = mock[GrepApiClient]

  override lazy val draftApiClient: DraftApiClient               = mock[DraftApiClient]
  override lazy val learningPathApiClient: LearningPathApiClient = mock[LearningPathApiClient]
  override lazy val articleApiClient: ArticleApiClient           = mock[ArticleApiClient]
  override lazy val draftConceptApiClient: DraftConceptApiClient = mock[DraftConceptApiClient]
  override lazy val feideApiClient: FeideApiClient               = mock[FeideApiClient]
  override lazy val redisClient: RedisClient                     = mock[RedisClient]
  override lazy val frontpageApiClient: FrontpageApiClient       = mock[FrontpageApiClient]
  override lazy val DBUtil: DBUtility                            = mock[DBUtility]

  override lazy val clock: SystemClock = mock[SystemClock]

  override lazy val converterService: ConverterService             = mock[ConverterService]
  override lazy val searchConverterService: SearchConverterService = mock[SearchConverterService]
  override lazy val multiSearchService: MultiSearchService         = mock[MultiSearchService]
  override lazy val grepSearchService: GrepSearchService           = mock[GrepSearchService]

  override lazy val articleIndexService: ArticleIndexService           = mock[ArticleIndexService]
  override lazy val learningPathIndexService: LearningPathIndexService = mock[LearningPathIndexService]
  override lazy val draftIndexService: DraftIndexService               = mock[DraftIndexService]
  override lazy val draftConceptIndexService: DraftConceptIndexService = mock[DraftConceptIndexService]
  override lazy val grepIndexService: GrepIndexService                 = mock[GrepIndexService]
  override lazy val nodeIndexService: NodeIndexService                 = mock[NodeIndexService]

  override lazy val multiDraftSearchService: MultiDraftSearchService = mock[MultiDraftSearchService]

  override def services: List[TapirController] = List()
  val swagger: SwaggerController               = mock[SwaggerController]
}
