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

class ComponentRegistry(properties: ConceptApiProperties)
    extends BaseComponentRegistry[ConceptApiProperties]
    with TapirApplication
    with DraftConceptController
    with PublishedConceptController
    with Clock
    with WriteService
    with ContentValidator
    with ReadService
    with ConverterService
    with StateTransitionRules
    with DraftConceptRepository
    with PublishedConceptRepository
    with DataSource
    with StrictLogging
    with DraftConceptSearchService
    with PublishedConceptSearchService
    with SearchService
    with SearchLanguage
    with SearchConverterService
    with Elastic4sClient
    with DraftConceptIndexService
    with PublishedConceptIndexService
    with IndexService
    with BaseIndexService
    with InternController
    with SearchApiClient
    with NdlaClient
    with Props
    with DBMigrator
    with ErrorHandling
    with SearchSettingsHelper
    with DraftSearchSettingsHelper
    with SwaggerDocControllerConfig
    with ConceptControllerHelpers {
  override lazy val props: ConceptApiProperties = properties
  override lazy val migrator: DBMigrator        = DBMigrator(
    new V23__SubjectNameAsTags(props),
    new V25__SubjectNameAsTagsPublished(props)
  )

  override lazy val dataSource: HikariDataSource = DataSource.getHikariDataSource

  override lazy val draftConceptRepository     = new DraftConceptRepository
  override lazy val publishedConceptRepository = new PublishedConceptRepository

  override lazy val draftConceptSearchService     = new DraftConceptSearchService
  override lazy val searchConverterService        = new SearchConverterService
  override lazy val draftConceptIndexService      = new DraftConceptIndexService
  override lazy val publishedConceptIndexService  = new PublishedConceptIndexService
  override lazy val publishedConceptSearchService = new PublishedConceptSearchService

  var e4sClient: NdlaE4sClient = Elastic4sClientFactory.getClient(props.SearchServer)

  override lazy val ndlaClient                       = new NdlaClient
  override lazy val searchApiClient                  = new SearchApiClient
  override lazy val myndlaApiClient: MyNDLAApiClient = new MyNDLAApiClient

  override lazy val writeService     = new WriteService
  override lazy val readService      = new ReadService
  override lazy val converterService = new ConverterService
  override lazy val clock            = new SystemClock
  override lazy val contentValidator = new ContentValidator

  lazy val draftConceptController                  = new DraftConceptController
  lazy val publishedConceptController              = new PublishedConceptController
  lazy val healthController: TapirHealthController = new TapirHealthController
  lazy val internController                        = new InternController

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
