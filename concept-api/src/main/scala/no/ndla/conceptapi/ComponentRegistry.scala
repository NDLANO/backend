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
import no.ndla.conceptapi.controller.*
import no.ndla.conceptapi.model.api.ErrorHandling
import no.ndla.conceptapi.model.search.{DraftSearchSettingsHelper, SearchSettingsHelper}
import no.ndla.conceptapi.repository.{DraftConceptRepository, PublishedConceptRepository}
import no.ndla.conceptapi.service.search.*
import no.ndla.conceptapi.service.*
import no.ndla.conceptapi.validation.ContentValidator
import no.ndla.network.NdlaClient
import no.ndla.search.{BaseIndexService, Elastic4sClient, SearchLanguage}
import no.ndla.common.Clock
import no.ndla.common.configuration.BaseComponentRegistry
import no.ndla.conceptapi.db.migrationwithdependencies.{V23__SubjectNameAsTags, V25__SubjectNameAsTagsPublished}
import no.ndla.database.{DBMigrator, DataSource}
import no.ndla.network.clients.SearchApiClient
import no.ndla.network.tapir.TapirApplication

class ComponentRegistry(properties: ConceptApiProperties) extends TapirApplication[ConceptApiProperties] {
  given props: ConceptApiProperties = properties
  given migrator: DBMigrator        = DBMigrator(
    new V23__SubjectNameAsTags(props),
    new V25__SubjectNameAsTagsPublished(props)
  )

  given dataSource: HikariDataSource = DataSource.getDataSource

  given draftConceptRepository     = new DraftConceptRepository
  given publishedConceptRepository = new PublishedConceptRepository

  given draftConceptSearchService     = new DraftConceptSearchService
  given searchConverterService        = new SearchConverterService
  given draftConceptIndexService      = new DraftConceptIndexService
  given publishedConceptIndexService  = new PublishedConceptIndexService
  given publishedConceptSearchService = new PublishedConceptSearchService

  var e4sClient: NdlaE4sClient = Elastic4sClientFactory.getClient(props.SearchServer)

  given ndlaClient                       = new NdlaClient
  given searchApiClient                  = new SearchApiClient
  given myndlaApiClient: MyNDLAApiClient = new MyNDLAApiClient

  given writeService     = new WriteService
  given readService      = new ReadService
  given converterService = new ConverterService
  given clock            = new SystemClock
  given contentValidator = new ContentValidator

  given draftConceptController                  = new DraftConceptController
  given publishedConceptController              = new PublishedConceptController
  given healthController: TapirHealthController = new TapirHealthController
  given internController                        = new InternController

  val swagger = new SwaggerController(
    List[TapirController](
      draftConceptController,
      publishedConceptController,
      healthController,
      internController
    ),
    SwaggerDocControllerConfig.swaggerInfo
  )

  override def services: List[TapirController] = swagger.getServices()

}
