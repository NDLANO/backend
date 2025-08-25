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
import no.ndla.conceptapi.model.search.{DraftSearchSettingsHelper, SearchSettingsHelper}
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
  given migrator: DBMigrator        = DBMigrator(
    new V23__SubjectNameAsTags(props),
    new V25__SubjectNameAsTagsPublished(props)
  )

  given dataSource: DataSource         = DataSource.getDataSource
  given errorHelpers: ErrorHelpers     = new ErrorHelpers
  given errorHandling: ErrorHandling   = new ControllerErrorHandling
  given searchLanguage: SearchLanguage = new SearchLanguage

  given draftConceptRepository: DraftConceptRepository         = new DraftConceptRepository
  given publishedConceptRepository: PublishedConceptRepository = new PublishedConceptRepository

  given draftConceptSearchService: DraftConceptSearchService         = new DraftConceptSearchService
  given searchConverterService: SearchConverterService               = new SearchConverterService
  given searchSettingsHelper: SearchSettingsHelper                   = new SearchSettingsHelper
  given draftSearchSettingsHelper: DraftSearchSettingsHelper         = new DraftSearchSettingsHelper
  given draftConceptIndexService: DraftConceptIndexService           = new DraftConceptIndexService
  given publishedConceptIndexService: PublishedConceptIndexService   = new PublishedConceptIndexService
  given publishedConceptSearchService: PublishedConceptSearchService = new PublishedConceptSearchService

  given e4sClient: NdlaE4sClient = Elastic4sClientFactory.getClient(props.SearchServer)

  given ndlaClient: NdlaClient           = new NdlaClient
  given searchApiClient: SearchApiClient = new SearchApiClient(props.SearchApiUrl)
  given myndlaApiClient: MyNDLAApiClient = new MyNDLAApiClient

  given stateTransitionRules: StateTransitionRules = new StateTransitionRules
  given writeService: WriteService                 = new WriteService
  given readService: ReadService                   = new ReadService
  given converterService: ConverterService         = new ConverterService
  given clock: Clock                               = new Clock
  given contentValidator: ContentValidator         = new ContentValidator

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
