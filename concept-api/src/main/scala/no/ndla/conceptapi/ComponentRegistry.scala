/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi

import no.ndla.common.Clock
import no.ndla.conceptapi.controller.*
import no.ndla.conceptapi.db.migrationwithdependencies.{V23__SubjectNameAsTags, V25__SubjectNameAsTagsPublished}
import no.ndla.conceptapi.model.domain.{DBConcept, DBPublishedConcept}
import no.ndla.conceptapi.repository.{DraftConceptRepository, PublishedConceptRepository}
import no.ndla.conceptapi.service.*
import no.ndla.conceptapi.service.search.*
import no.ndla.conceptapi.validation.ContentValidator
import no.ndla.database.{DBMigrator, DBUtility, DataSource}
import no.ndla.network.NdlaClient
import no.ndla.network.clients.{MyNDLAApiClient, SearchApiClient}
import no.ndla.network.jwt.{DefaultJwsKeySelectorFactory, JwsKeySelectorFactory}
import no.ndla.network.tapir.auth.NdlaAuth
import no.ndla.network.tapir.*
import no.ndla.search.{Elastic4sClientFactory, NdlaE4sClient, SearchLanguage}

class ComponentRegistry(properties: ConceptApiProperties) extends TapirApplication[ConceptApiProperties] {
  given props: ConceptApiProperties          = properties
  given dbConcept: DBConcept                 = new DBConcept
  given publishedConcept: DBPublishedConcept = new DBPublishedConcept

  given clock: Clock                       = new Clock
  given e4sClient: NdlaE4sClient           = Elastic4sClientFactory.getClient(props.SearchServer)
  given dataSource: DataSource             = DataSource.getDataSource
  given errorHelpers: ErrorHelpers         = new ErrorHelpers
  given errorHandling: ErrorHandling       = new ControllerErrorHandling
  given searchLanguage: SearchLanguage     = new SearchLanguage
  given converterService: ConverterService = new ConverterService
  given dbUtility: DBUtility               = new DBUtility

  given migrator: DBMigrator = DBMigrator(new V23__SubjectNameAsTags(props), new V25__SubjectNameAsTagsPublished(props))

  given draftConceptRepository: DraftConceptRepository         = new DraftConceptRepository
  given publishedConceptRepository: PublishedConceptRepository = new PublishedConceptRepository

  given searchConverterService: SearchConverterService               = new SearchConverterService
  given publishedConceptIndexService: PublishedConceptIndexService   = new PublishedConceptIndexService
  given publishedConceptSearchService: PublishedConceptSearchService = new PublishedConceptSearchService
  given draftConceptIndexService: DraftConceptIndexService           = new DraftConceptIndexService
  given draftConceptSearchService: DraftConceptSearchService         = new DraftConceptSearchService

  given ndlaClient: NdlaClient                        = new NdlaClient
  given searchApiClient: SearchApiClient              = new SearchApiClient(props.SearchApiUrl)
  protected def buildMyNDLAApiClient: MyNDLAApiClient = new MyNDLAApiClient
  given myndlaApiClient: MyNDLAApiClient              = buildMyNDLAApiClient

  given jwsKeySelectorFactory: JwsKeySelectorFactory = DefaultJwsKeySelectorFactory
  given ndlaAuth: NdlaAuth                           = NdlaAuth()

  implicit lazy val stateTransitionRules: StateTransitionRules = new StateTransitionRules
  implicit lazy val writeService: WriteService                 = new WriteService
  given readService: ReadService                               = new ReadService
  given contentValidator: ContentValidator                     = new ContentValidator

  given conceptControllerHelpers: ConceptControllerHelpers = new ConceptControllerHelpers

  given draftConceptController: DraftConceptController         = new DraftConceptController
  given publishedConceptController: PublishedConceptController = new PublishedConceptController
  given healthController: TapirHealthController                = new TapirHealthController
  given internController: InternController                     = new InternController

  given swaggerInfo: SwaggerInfo   = SwaggerInfo(prefix = "concept-api", description = "Services for accessing concepts")
  given swagger: SwaggerController = new SwaggerController(
    draftConceptController,
    publishedConceptController,
    healthController,
    internController,
    new LegacyPrefixAlias(internController, "intern"),
  )

  given services: List[TapirController] = swagger.allServices
  given routes: Routes                  = new Routes
}
