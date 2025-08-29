/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi

import no.ndla.conceptapi.controller.*
import no.ndla.conceptapi.repository.{DraftConceptRepository, PublishedConceptRepository}
import no.ndla.conceptapi.service.search.*
import no.ndla.conceptapi.service.*
import no.ndla.conceptapi.validation.ContentValidator
import no.ndla.network.NdlaClient
import no.ndla.search.{Elastic4sClientFactory, NdlaE4sClient, SearchLanguage}
import no.ndla.common.Clock
import no.ndla.conceptapi.db.migrationwithdependencies.{V23__SubjectNameAsTags, V25__SubjectNameAsTagsPublished}
import no.ndla.database.{DBMigrator, DataSource}
import no.ndla.network.clients.{MyNDLAApiClient, SearchApiClient}
import no.ndla.network.tapir.{
  ErrorHandling,
  ErrorHelpers,
  Routes,
  SwaggerController,
  SwaggerInfo,
  TapirApplication,
  TapirController,
  TapirHealthController
}
import sttp.tapir.stringToPath
import no.ndla.network.tapir.auth.Permission

class ComponentRegistry(properties: ConceptApiProperties) extends TapirApplication[ConceptApiProperties] {
  given props: ConceptApiProperties = properties

  given clock: Clock                       = new Clock
  given e4sClient: NdlaE4sClient           = Elastic4sClientFactory.getClient(props.SearchServer)
  given dataSource: DataSource             = DataSource.getDataSource
  given errorHelpers: ErrorHelpers         = new ErrorHelpers
  given errorHandling: ErrorHandling       = new ControllerErrorHandling
  given searchLanguage: SearchLanguage     = new SearchLanguage
  given converterService: ConverterService = new ConverterService

  given migrator: DBMigrator = DBMigrator(
    new V23__SubjectNameAsTags(props),
    new V25__SubjectNameAsTagsPublished(props)
  )

  given draftConceptRepository: DraftConceptRepository         = new DraftConceptRepository
  given publishedConceptRepository: PublishedConceptRepository = new PublishedConceptRepository

  given searchConverterService: SearchConverterService               = new SearchConverterService
  given publishedConceptIndexService: PublishedConceptIndexService   = new PublishedConceptIndexService
  given publishedConceptSearchService: PublishedConceptSearchService = new PublishedConceptSearchService
  given draftConceptIndexService: DraftConceptIndexService           = new DraftConceptIndexService
  given draftConceptSearchService: DraftConceptSearchService         = new DraftConceptSearchService

  given ndlaClient: NdlaClient           = new NdlaClient
  given searchApiClient: SearchApiClient = new SearchApiClient(props.SearchApiUrl)
  given myndlaApiClient: MyNDLAApiClient = new MyNDLAApiClient

  implicit lazy val stateTransitionRules: StateTransitionRules = new StateTransitionRules
  implicit lazy val writeService: WriteService                 = new WriteService
  given readService: ReadService                               = new ReadService
  given contentValidator: ContentValidator                     = new ContentValidator

  given conceptControllerHelpers: ConceptControllerHelpers = new ConceptControllerHelpers

  given draftConceptController: DraftConceptController         = new DraftConceptController
  given publishedConceptController: PublishedConceptController = new PublishedConceptController
  given healthController: TapirHealthController                = new TapirHealthController
  given internController: InternController                     = new InternController

  private val swaggerInfo = SwaggerInfo(
    mountPoint = "concept-api" / "api-docs",
    description = "Services for accessing concepts",
    authUrl = props.Auth0LoginEndpoint,
    scopes = Permission.toSwaggerMap(Permission.thatStartsWith("concept"))
  )

  given swagger: SwaggerController = new SwaggerController(
    List(
      draftConceptController,
      publishedConceptController,
      healthController,
      internController
    ),
    swaggerInfo
  )

  given services: List[TapirController] = swagger.getServices()
  given routes: Routes                  = new Routes
}
