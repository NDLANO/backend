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
    with MockitoSugar
    with StrictLogging {
  given props: ConceptApiProperties = new ConceptApiProperties {
    override def IntroductionHtmlTags: Set[String] = Set("br", "code", "em", "p", "span", "strong", "sub", "sup")
  }

  given migrator: DBMigrator                                   = mock[DBMigrator]
  given draftConceptRepository: DraftConceptRepository         = mock[DraftConceptRepository]
  given publishedConceptRepository: PublishedConceptRepository = mock[PublishedConceptRepository]

  given draftConceptController: DraftConceptController         = mock[DraftConceptController]
  given publishedConceptController: PublishedConceptController = mock[PublishedConceptController]
  given internController: InternController                     = mock[InternController]
  given healthController: TapirHealthController                = mock[TapirHealthController]

  given searchConverterService: SearchConverterService               = mock[SearchConverterService]
  given draftConceptIndexService: DraftConceptIndexService           = mock[DraftConceptIndexService]
  given draftConceptSearchService: DraftConceptSearchService         = mock[DraftConceptSearchService]
  given publishedConceptIndexService: PublishedConceptIndexService   = mock[PublishedConceptIndexService]
  given publishedConceptSearchService: PublishedConceptSearchService = mock[PublishedConceptSearchService]

  var e4sClient: NdlaE4sClient                         = mock[NdlaE4sClient]
  val mockitoSugar: MockitoSugar                       = mock[MockitoSugar]
  given dataSource: HikariDataSource       = mock[HikariDataSource]
  given writeService: WriteService         = mock[WriteService]
  given readService: ReadService           = mock[ReadService]
  given converterService: ConverterService = mock[ConverterService]
  given contentValidator: ContentValidator = mock[ContentValidator]
  given clock: SystemClock                 = mock[SystemClock]

  given ndlaClient: NdlaClient           = mock[NdlaClient]
  given myndlaApiClient: MyNDLAApiClient = mock[MyNDLAApiClient]
  given searchApiClient: SearchApiClient = mock[SearchApiClient]

  def services: List[TapirController] = List.empty
  val swagger: SwaggerController      = mock[SwaggerController]
}
