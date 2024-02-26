/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi

import com.typesafe.scalalogging.StrictLogging
import com.zaxxer.hikari.HikariDataSource
import no.ndla.conceptapi.controller._
import no.ndla.conceptapi.integration.{ArticleApiClient, DataSource, TaxonomyApiClient}
import no.ndla.conceptapi.model.api.ErrorHelpers
import no.ndla.conceptapi.model.search.{DraftSearchSettingsHelper, SearchSettingsHelper}
import no.ndla.conceptapi.repository.{DraftConceptRepository, PublishedConceptRepository}
import no.ndla.conceptapi.service.search._
import no.ndla.conceptapi.service._
import no.ndla.conceptapi.validation.ContentValidator
import no.ndla.network.NdlaClient
import no.ndla.search.{BaseIndexService, Elastic4sClient}
import no.ndla.common.Clock
import no.ndla.common.configuration.BaseComponentRegistry
import no.ndla.network.tapir.{
  NdlaMiddleware,
  Routes,
  Service,
  SwaggerControllerConfig,
  TapirErrorHelpers,
  TapirHealthController
}

class ComponentRegistry(properties: ConceptApiProperties)
    extends BaseComponentRegistry[ConceptApiProperties]
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
    with ImportService
    with SearchService
    with SearchConverterService
    with Elastic4sClient
    with DraftConceptIndexService
    with PublishedConceptIndexService
    with IndexService
    with BaseIndexService
    with InternController
    with ArticleApiClient
    with NdlaClient
    with Props
    with DBMigrator
    with ErrorHelpers
    with SearchSettingsHelper
    with DraftSearchSettingsHelper
    with TaxonomyApiClient
    with Routes[Eff]
    with NdlaMiddleware
    with TapirErrorHelpers
    with SwaggerControllerConfig
    with SwaggerDocControllerConfig
    with TapirHealthController
    with ConceptControllerHelpers {
  override val props: ConceptApiProperties = properties
  override val migrator                    = new DBMigrator

  override val dataSource: HikariDataSource = DataSource.getHikariDataSource
  DataSource.connectToDatabase()

  lazy val draftConceptRepository     = new DraftConceptRepository
  lazy val publishedConceptRepository = new PublishedConceptRepository

  lazy val draftConceptSearchService     = new DraftConceptSearchService
  lazy val searchConverterService        = new SearchConverterService
  lazy val draftConceptIndexService      = new DraftConceptIndexService
  lazy val publishedConceptIndexService  = new PublishedConceptIndexService
  lazy val publishedConceptSearchService = new PublishedConceptSearchService

  lazy val taxonomyApiClient: TaxonomyApiClient = new TaxonomyApiClient

  var e4sClient: NdlaE4sClient = Elastic4sClientFactory.getClient(props.SearchServer)

  lazy val ndlaClient       = new NdlaClient
  lazy val articleApiClient = new ArticleApiClient

  lazy val importService = new ImportService

  lazy val writeService     = new WriteService
  lazy val readService      = new ReadService
  lazy val converterService = new ConverterService
  lazy val clock            = new SystemClock
  lazy val contentValidator = new ContentValidator

  lazy val draftConceptController     = new DraftConceptController
  lazy val publishedConceptController = new PublishedConceptController
  lazy val healthController           = new TapirHealthController[Eff]
  lazy val internController           = new InternController

  private val swagger = new SwaggerController(
    List[Service[Eff]](
      draftConceptController,
      publishedConceptController,
      healthController,
      internController
    ),
    SwaggerDocControllerConfig.swaggerInfo
  )

  override val services: List[Service[Eff]] = swagger.getServices()

}
