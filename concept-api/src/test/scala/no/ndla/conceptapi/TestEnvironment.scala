/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi

import com.typesafe.scalalogging.StrictLogging
import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.Clock
import no.ndla.conceptapi.controller.{DraftConceptController, NdlaController, PublishedConceptController}
import no.ndla.conceptapi.integration.{ArticleApiClient, DataSource, TaxonomyApiClient}
import no.ndla.conceptapi.model.api.ErrorHelpers
import no.ndla.conceptapi.model.domain.DBConcept
import no.ndla.conceptapi.model.search.{DraftSearchSettingsHelper, SearchSettingsHelper}
import no.ndla.conceptapi.repository.{DraftConceptRepository, PublishedConceptRepository}
import no.ndla.conceptapi.service._
import no.ndla.conceptapi.service.search._
import no.ndla.conceptapi.validation.ContentValidator
import no.ndla.network.NdlaClient
import no.ndla.network.scalatra.{NdlaControllerBase, NdlaSwaggerSupport}
import no.ndla.search.{BaseIndexService, Elastic4sClient}
import org.mockito.scalatest.MockitoSugar

trait TestEnvironment
    extends DraftConceptRepository
    with PublishedConceptRepository
    with DraftConceptController
    with NdlaControllerBase
    with NdlaSwaggerSupport
    with DBConcept
    with PublishedConceptController
    with NdlaController
    with SearchConverterService
    with PublishedConceptSearchService
    with PublishedConceptIndexService
    with DraftConceptIndexService
    with DraftConceptSearchService
    with IndexService
    with TaxonomyApiClient
    with BaseIndexService
    with Elastic4sClient
    with SearchService
    with StrictLogging
    with MockitoSugar
    with DataSource
    with WriteService
    with ReadService
    with ConverterService
    with StateTransitionRules
    with ContentValidator
    with ImportService
    with ArticleApiClient
    with NdlaClient
    with Clock
    with Props
    with ErrorHelpers
    with SearchSettingsHelper
    with DraftSearchSettingsHelper
    with DBMigrator
    with ConceptApiInfo {
  override val props: ConceptApiProperties = new ConceptApiProperties {
    override def IntroductionHtmlTags: Set[String] = Set("br", "code", "em", "p", "span", "strong", "sub", "sup")
  }

  val migrator: DBMigrator                                   = mock[DBMigrator]
  val draftConceptRepository: DraftConceptRepository         = mock[DraftConceptRepository]
  val publishedConceptRepository: PublishedConceptRepository = mock[PublishedConceptRepository]

  val draftConceptController: DraftConceptController         = mock[DraftConceptController]
  val publishedConceptController: PublishedConceptController = mock[PublishedConceptController]

  val searchConverterService: SearchConverterService               = mock[SearchConverterService]
  val draftConceptIndexService: DraftConceptIndexService           = mock[DraftConceptIndexService]
  val draftConceptSearchService: DraftConceptSearchService         = mock[DraftConceptSearchService]
  val publishedConceptIndexService: PublishedConceptIndexService   = mock[PublishedConceptIndexService]
  val publishedConceptSearchService: PublishedConceptSearchService = mock[PublishedConceptSearchService]

  val taxonomyApiClient: TaxonomyApiClient = mock[TaxonomyApiClient]

  var e4sClient: NdlaE4sClient           = mock[NdlaE4sClient]
  val mockitoSugar: MockitoSugar         = mock[MockitoSugar]
  val dataSource: HikariDataSource       = mock[HikariDataSource]
  val writeService: WriteService         = mock[WriteService]
  val readService: ReadService           = mock[ReadService]
  val converterService: ConverterService = mock[ConverterService]
  val contentValidator: ContentValidator = mock[ContentValidator]
  val clock: SystemClock                 = mock[SystemClock]
  val importService: ImportService       = mock[ImportService]

  val ndlaClient: NdlaClient             = mock[NdlaClient]
  val articleApiClient: ArticleApiClient = mock[ArticleApiClient]

}
