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
import no.ndla.common.util.TraitUtil
import no.ndla.database.{DBUtility, DatabaseProps}
import no.ndla.network.NdlaClient
import no.ndla.network.clients.{FeideApiClient, FrontpageApiClient, MyNDLAApiClient, RedisClient, TaxonomyApiClient}
import no.ndla.network.tapir.{SwaggerController, TapirController, TapirHealthController}
import no.ndla.scalatestsuite.DBUtilityStub
import no.ndla.search.{NdlaE4sClient, SearchLanguage}
import no.ndla.searchapi.controller.{InternController, SearchController}
import no.ndla.searchapi.integration.*
import no.ndla.searchapi.service.search.*
import no.ndla.searchapi.service.ConverterService
import org.scalatestplus.mockito.MockitoSugar

class TestProps extends SearchApiProperties with BaseProps with DatabaseProps {
  override def MetaMigrationLocation: String = ???
}

trait TestEnvironment extends MockitoSugar with StrictLogging {
  implicit lazy val props: TestProps = new TestProps

  implicit lazy val searchController: SearchController      = mock[SearchController]
  implicit lazy val internController: InternController      = mock[InternController]
  implicit lazy val healthController: TapirHealthController = mock[TapirHealthController]

  implicit lazy val ndlaClient: NdlaClient   = mock[NdlaClient]
  implicit lazy val e4sClient: NdlaE4sClient = mock[NdlaE4sClient]

  implicit lazy val myndlaApiClient: MyNDLAApiClient = mock[MyNDLAApiClient]

  implicit lazy val taxonomyApiClient: TaxonomyApiClient = mock[TaxonomyApiClient]
  implicit lazy val grepApiClient: GrepApiClient         = mock[GrepApiClient]

  implicit lazy val draftApiClient: DraftApiClient               = mock[DraftApiClient]
  implicit lazy val learningPathApiClient: LearningPathApiClient = mock[LearningPathApiClient]
  implicit lazy val articleApiClient: ArticleApiClient           = mock[ArticleApiClient]
  implicit lazy val draftConceptApiClient: DraftConceptApiClient = mock[DraftConceptApiClient]
  implicit lazy val feideApiClient: FeideApiClient               = mock[FeideApiClient]
  implicit lazy val redisClient: RedisClient                     = mock[RedisClient]
  implicit lazy val frontpageApiClient: FrontpageApiClient       = mock[FrontpageApiClient]
  implicit lazy val DBUtil: DBUtility                            = DBUtilityStub()
  implicit lazy val searchLanguage: SearchLanguage               = mock[SearchLanguage]
  implicit lazy val traitUtil: TraitUtil                         = mock[TraitUtil]

  implicit lazy val clock: Clock = mock[Clock]

  implicit lazy val converterService: ConverterService             = mock[ConverterService]
  implicit lazy val searchConverterService: SearchConverterService = mock[SearchConverterService]
  implicit lazy val multiSearchService: MultiSearchService         = mock[MultiSearchService]
  implicit lazy val grepSearchService: GrepSearchService           = mock[GrepSearchService]

  implicit lazy val articleIndexService: ArticleIndexService           = mock[ArticleIndexService]
  implicit lazy val learningPathIndexService: LearningPathIndexService = mock[LearningPathIndexService]
  implicit lazy val draftIndexService: DraftIndexService               = mock[DraftIndexService]
  implicit lazy val draftConceptIndexService: DraftConceptIndexService = mock[DraftConceptIndexService]
  implicit lazy val grepIndexService: GrepIndexService                 = mock[GrepIndexService]
  implicit lazy val nodeIndexService: NodeIndexService                 = mock[NodeIndexService]

  implicit lazy val multiDraftSearchService: MultiDraftSearchService = mock[MultiDraftSearchService]

  implicit def services: List[TapirController] = List()
  implicit lazy val swagger: SwaggerController = mock[SwaggerController]
}
