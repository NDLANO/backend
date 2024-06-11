/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi

import com.typesafe.scalalogging.StrictLogging
import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.Clock
import no.ndla.conceptapi.controller.{
  ConceptControllerHelpers,
  DraftConceptController,
  InternController,
  PublishedConceptController
}
import no.ndla.conceptapi.integration.{ArticleApiClient, DataSource, SearchApiClient, TaxonomyApiClient}
import no.ndla.conceptapi.model.api.ErrorHelpers
import no.ndla.conceptapi.model.search.{DraftSearchSettingsHelper, SearchSettingsHelper}
import no.ndla.conceptapi.repository.{DraftConceptRepository, PublishedConceptRepository}
import no.ndla.conceptapi.service.*
import no.ndla.conceptapi.service.search.*
import no.ndla.conceptapi.validation.ContentValidator
import no.ndla.network.NdlaClient
import no.ndla.network.tapir.{NdlaMiddleware, Routes, Service}
import no.ndla.search.{BaseIndexService, Elastic4sClient}
import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment
    extends DraftConceptRepository
    with PublishedConceptRepository
    with DraftConceptController
    with ConceptControllerHelpers
    with PublishedConceptController
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
    with SearchApiClient
    with NdlaClient
    with Clock
    with Props
    with ErrorHelpers
    with SearchSettingsHelper
    with DraftSearchSettingsHelper
    with DBMigrator
    with Routes[Eff]
    with NdlaMiddleware
    with InternController {
  override val props: ConceptApiProperties = new ConceptApiProperties {
    override def IntroductionHtmlTags: Set[String] = Set("br", "code", "em", "p", "span", "strong", "sub", "sup")
  }

  val migrator: DBMigrator                                   = mock[DBMigrator]
  val draftConceptRepository: DraftConceptRepository         = mock[DraftConceptRepository]
  val publishedConceptRepository: PublishedConceptRepository = mock[PublishedConceptRepository]

  val draftConceptController: DraftConceptController         = mock[DraftConceptController]
  val publishedConceptController: PublishedConceptController = mock[PublishedConceptController]
  val internController: InternController                     = mock[InternController]

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
  val searchApiClient: SearchApiClient   = mock[SearchApiClient]

  def services: List[Service[Eff]] = List.empty
}
