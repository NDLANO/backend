/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.Clock
import no.ndla.conceptapi.controller.{
  ConceptControllerHelpers,
  DraftConceptController,
  InternController,
  PublishedConceptController
}
import no.ndla.conceptapi.repository.{DraftConceptRepository, PublishedConceptRepository}
import no.ndla.conceptapi.service.*
import no.ndla.conceptapi.service.search.*
import no.ndla.conceptapi.validation.ContentValidator
import no.ndla.database.{DBMigrator, DataSource}
import no.ndla.network.NdlaClient
import no.ndla.network.clients.{MyNDLAApiClient, SearchApiClient}
import no.ndla.network.tapir.{
  ErrorHandling,
  ErrorHelpers,
  Routes,
  SwaggerController,
  TapirApplication,
  TapirController,
  TapirHealthController
}
import no.ndla.search.{NdlaE4sClient, SearchLanguage}
import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment extends TapirApplication[ConceptApiProperties] with MockitoSugar with StrictLogging {
  implicit lazy val props: ConceptApiProperties = new ConceptApiProperties {
    override def IntroductionHtmlTags: Set[String] = Set("br", "code", "em", "p", "span", "strong", "sub", "sup")
  }

  implicit lazy val migrator: DBMigrator                                   = mock[DBMigrator]
  implicit lazy val draftConceptRepository: DraftConceptRepository         = mock[DraftConceptRepository]
  implicit lazy val publishedConceptRepository: PublishedConceptRepository = mock[PublishedConceptRepository]

  implicit lazy val draftConceptController: DraftConceptController         = mock[DraftConceptController]
  implicit lazy val publishedConceptController: PublishedConceptController = mock[PublishedConceptController]
  implicit lazy val internController: InternController                     = mock[InternController]
  implicit lazy val healthController: TapirHealthController                = mock[TapirHealthController]

  implicit lazy val searchConverterService: SearchConverterService               = mock[SearchConverterService]
  implicit lazy val draftConceptIndexService: DraftConceptIndexService           = mock[DraftConceptIndexService]
  implicit lazy val draftConceptSearchService: DraftConceptSearchService         = mock[DraftConceptSearchService]
  implicit lazy val publishedConceptIndexService: PublishedConceptIndexService   = mock[PublishedConceptIndexService]
  implicit lazy val publishedConceptSearchService: PublishedConceptSearchService = mock[PublishedConceptSearchService]

  implicit lazy val e4sClient: NdlaE4sClient           = mock[NdlaE4sClient]
  implicit lazy val dataSource: DataSource             = mock[DataSource]
  implicit lazy val writeService: WriteService         = mock[WriteService]
  implicit lazy val readService: ReadService           = mock[ReadService]
  implicit lazy val converterService: ConverterService = mock[ConverterService]
  implicit lazy val contentValidator: ContentValidator = mock[ContentValidator]
  implicit lazy val clock: Clock                       = mock[Clock]
  implicit lazy val errorHelpers: ErrorHelpers         = new ErrorHelpers
  implicit lazy val errorHandling: ErrorHandling       = mock[ErrorHandling]
  implicit lazy val searchLanguage: SearchLanguage     = mock[SearchLanguage]

  implicit lazy val ndlaClient: NdlaClient           = mock[NdlaClient]
  implicit lazy val myndlaApiClient: MyNDLAApiClient = mock[MyNDLAApiClient]
  implicit lazy val searchApiClient: SearchApiClient = mock[SearchApiClient]

  implicit lazy val stateTransitionRules: StateTransitionRules         = mock[StateTransitionRules]
  implicit lazy val conceptControllerHelpers: ConceptControllerHelpers = mock[ConceptControllerHelpers]

  implicit lazy val services: List[TapirController] = List.empty
  implicit lazy val swagger: SwaggerController      = mock[SwaggerController]
  implicit lazy val routes: Routes                  = mock[Routes]
}
