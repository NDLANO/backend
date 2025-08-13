/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
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
import no.ndla.conceptapi.model.api.ErrorHandling
import no.ndla.conceptapi.model.search.{DraftSearchSettingsHelper, SearchSettingsHelper}
import no.ndla.conceptapi.repository.{DraftConceptRepository, PublishedConceptRepository}
import no.ndla.conceptapi.service.*
import no.ndla.conceptapi.service.search.*
import no.ndla.conceptapi.validation.ContentValidator
import no.ndla.database.{DBMigrator, DataSource}
import no.ndla.network.NdlaClient
import no.ndla.network.clients.SearchApiClient
import no.ndla.network.tapir.TapirApplication
import no.ndla.search.{BaseIndexService, Elastic4sClient, SearchLanguage}
import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment
    extends TapirApplication
    with DraftConceptRepository
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
    with SearchApiClient
    with NdlaClient
    with Clock
    with Props
    with ErrorHandling
    with SearchSettingsHelper
    with SearchLanguage
    with DraftSearchSettingsHelper
    with DBMigrator
    with InternController {
  override lazy val props: ConceptApiProperties = new ConceptApiProperties {
    override def IntroductionHtmlTags: Set[String] = Set("br", "code", "em", "p", "span", "strong", "sub", "sup")
  }

  override lazy val migrator: DBMigrator                                   = mock[DBMigrator]
  override lazy val draftConceptRepository: DraftConceptRepository         = mock[DraftConceptRepository]
  override lazy val publishedConceptRepository: PublishedConceptRepository = mock[PublishedConceptRepository]

  override lazy val draftConceptController: DraftConceptController         = mock[DraftConceptController]
  override lazy val publishedConceptController: PublishedConceptController = mock[PublishedConceptController]
  override lazy val internController: InternController                     = mock[InternController]
  override lazy val healthController: TapirHealthController                = mock[TapirHealthController]

  override lazy val searchConverterService: SearchConverterService               = mock[SearchConverterService]
  override lazy val draftConceptIndexService: DraftConceptIndexService           = mock[DraftConceptIndexService]
  override lazy val draftConceptSearchService: DraftConceptSearchService         = mock[DraftConceptSearchService]
  override lazy val publishedConceptIndexService: PublishedConceptIndexService   = mock[PublishedConceptIndexService]
  override lazy val publishedConceptSearchService: PublishedConceptSearchService = mock[PublishedConceptSearchService]

  var e4sClient: NdlaE4sClient                         = mock[NdlaE4sClient]
  val mockitoSugar: MockitoSugar                       = mock[MockitoSugar]
  override lazy val dataSource: HikariDataSource       = mock[HikariDataSource]
  override lazy val writeService: WriteService         = mock[WriteService]
  override lazy val readService: ReadService           = mock[ReadService]
  override lazy val converterService: ConverterService = mock[ConverterService]
  override lazy val contentValidator: ContentValidator = mock[ContentValidator]
  override lazy val clock: SystemClock                 = mock[SystemClock]

  override lazy val ndlaClient: NdlaClient           = mock[NdlaClient]
  override lazy val myndlaApiClient: MyNDLAApiClient = mock[MyNDLAApiClient]
  override lazy val searchApiClient: SearchApiClient = mock[SearchApiClient]

  def services: List[TapirController] = List.empty
  val swagger: SwaggerController      = mock[SwaggerController]
}
