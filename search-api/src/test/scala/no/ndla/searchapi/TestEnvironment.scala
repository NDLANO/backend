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

trait TestEnvironment extends TapirApplication with MockitoSugar with StrictLogging {
  given props: TestProps = new TestProps

  given searchController: SearchController      = mock[SearchController]
  given internController: InternController      = mock[InternController]
  given healthController: TapirHealthController = mock[TapirHealthController]

  given ndlaClient: NdlaClient = mock[NdlaClient]
  var e4sClient: NdlaE4sClient = mock[NdlaE4sClient]

  given myndlaApiClient: MyNDLAApiClient = mock[MyNDLAApiClient]

  given taxonomyApiClient: TaxonomyApiClient = mock[TaxonomyApiClient]
  given grepApiClient: GrepApiClient         = mock[GrepApiClient]

  given draftApiClient: DraftApiClient               = mock[DraftApiClient]
  given learningPathApiClient: LearningPathApiClient = mock[LearningPathApiClient]
  given articleApiClient: ArticleApiClient           = mock[ArticleApiClient]
  given draftConceptApiClient: DraftConceptApiClient = mock[DraftConceptApiClient]
  given feideApiClient: FeideApiClient               = mock[FeideApiClient]
  given redisClient: RedisClient                     = mock[RedisClient]
  given frontpageApiClient: FrontpageApiClient       = mock[FrontpageApiClient]
  given DBUtil: DBUtility                            = mock[DBUtility]

  given clock: SystemClock = mock[SystemClock]

  given converterService: ConverterService             = mock[ConverterService]
  given searchConverterService: SearchConverterService = mock[SearchConverterService]
  given multiSearchService: MultiSearchService         = mock[MultiSearchService]
  given grepSearchService: GrepSearchService           = mock[GrepSearchService]

  given articleIndexService: ArticleIndexService           = mock[ArticleIndexService]
  given learningPathIndexService: LearningPathIndexService = mock[LearningPathIndexService]
  given draftIndexService: DraftIndexService               = mock[DraftIndexService]
  given draftConceptIndexService: DraftConceptIndexService = mock[DraftConceptIndexService]
  given grepIndexService: GrepIndexService                 = mock[GrepIndexService]
  given nodeIndexService: NodeIndexService                 = mock[NodeIndexService]

  given multiDraftSearchService: MultiDraftSearchService = mock[MultiDraftSearchService]

  override def services: List[TapirController] = List()
  val swagger: SwaggerController               = mock[SwaggerController]
}
