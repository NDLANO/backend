/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi

import com.typesafe.scalalogging.StrictLogging
import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.Clock
import no.ndla.conceptapi.controller.{DraftConceptController, NdlaController, PublishedConceptController}
import no.ndla.conceptapi.integration.{ArticleApiClient, DataSource, TaxonomyApiClient}
import no.ndla.conceptapi.model.api.ErrorHelpers
import no.ndla.conceptapi.model.domain.DBConcept
import no.ndla.conceptapi.model.search.{DraftSearchSettingsHelper, SearchSettingsHelper}
import no.ndla.conceptapi.repository.{DraftConceptRepository, PublishedConceptRepository}
import no.ndla.conceptapi.service._
import no.ndla.conceptapi.service.search._
import no.ndla.conceptapi.validation.ContentValidator
import no.ndla.network.NdlaClient
import no.ndla.network.scalatra.{NdlaControllerBase, NdlaSwaggerSupport}
import no.ndla.search.{BaseIndexService, Elastic4sClient}
import org.mockito.scalatest.MockitoSugar

trait TestEnvironment
    extends DraftConceptRepository
    with PublishedConceptRepository
    with DraftConceptController
    with NdlaControllerBase
    with NdlaSwaggerSupport
    with DBConcept
    with PublishedConceptController
    with NdlaController
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
    with NdlaClient
    with Clock
    with Props
    with ErrorHelpers
    with SearchSettingsHelper
    with DraftSearchSettingsHelper
    with DBMigrator
    with ConceptApiInfo {
  override val props = new ConceptApiProperties

  val migrator                   = mock[DBMigrator]
  val draftConceptRepository     = mock[DraftConceptRepository]
  val publishedConceptRepository = mock[PublishedConceptRepository]

  val draftConceptController     = mock[DraftConceptController]
  val publishedConceptController = mock[PublishedConceptController]

  val searchConverterService        = mock[SearchConverterService]
  val draftConceptIndexService      = mock[DraftConceptIndexService]
  val draftConceptSearchService     = mock[DraftConceptSearchService]
  val publishedConceptIndexService  = mock[PublishedConceptIndexService]
  val publishedConceptSearchService = mock[PublishedConceptSearchService]

  val taxonomyApiClient = mock[TaxonomyApiClient]

  var e4sClient        = mock[NdlaE4sClient]
  val mockitoSugar     = mock[MockitoSugar]
  val dataSource       = mock[HikariDataSource]
  val writeService     = mock[WriteService]
  val readService      = mock[ReadService]
  val converterService = mock[ConverterService]
  val contentValidator = mock[ContentValidator]
  val clock            = mock[SystemClock]
  val importService    = mock[ImportService]

  val ndlaClient       = mock[NdlaClient]
  val articleApiClient = mock[ArticleApiClient]

}
