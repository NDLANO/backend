/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi

import com.typesafe.scalalogging.LazyLogging
import com.zaxxer.hikari.HikariDataSource
import no.ndla.conceptapi.auth.User
import no.ndla.conceptapi.controller.{
  DraftConceptController,
  DraftNdlaController,
  NdlaController,
  PublishedConceptController
}
import no.ndla.conceptapi.integration.{ArticleApiClient, DataSource, ImageApiClient}
import no.ndla.conceptapi.model.api.ErrorHelpers
import no.ndla.conceptapi.model.domain.DBConcept
import no.ndla.conceptapi.model.search.{DraftSearchSettingsHelper, SearchSettingsHelper}
import no.ndla.conceptapi.repository.{DraftConceptRepository, PublishedConceptRepository}
import no.ndla.conceptapi.service.search.{
  DraftConceptIndexService,
  DraftConceptSearchService,
  IndexService,
  PublishedConceptIndexService,
  PublishedConceptSearchService,
  SearchConverterService,
  SearchService
}
import no.ndla.conceptapi.service.{ConverterService, ImportService, ReadService, StateTransitionRules, WriteService}
import no.ndla.conceptapi.validation.ContentValidator
import no.ndla.network.NdlaClient
import no.ndla.search.{BaseIndexService, Elastic4sClient, NdlaE4sClient}
import no.ndla.common.Clock
import org.mockito.scalatest.MockitoSugar

trait TestEnvironment
    extends DraftConceptRepository
    with PublishedConceptRepository
    with DraftConceptController
    with DBConcept
    with PublishedConceptController
    with NdlaController
    with DraftNdlaController
    with SearchConverterService
    with PublishedConceptSearchService
    with PublishedConceptIndexService
    with DraftConceptIndexService
    with DraftConceptSearchService
    with IndexService
    with BaseIndexService
    with Elastic4sClient
    with SearchService
    with LazyLogging
    with MockitoSugar
    with DataSource
    with WriteService
    with ReadService
    with ConverterService
    with StateTransitionRules
    with ContentValidator
    with ImportService
    with ArticleApiClient
    with ImageApiClient
    with NdlaClient
    with Clock
    with User
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

  var e4sClient        = mock[NdlaE4sClient]
  val lazyLogging      = mock[LazyLogging]
  val mockitoSugar     = mock[MockitoSugar]
  val dataSource       = mock[HikariDataSource]
  val writeService     = mock[WriteService]
  val readService      = mock[ReadService]
  val converterService = mock[ConverterService]
  val contentValidator = mock[ContentValidator]
  val clock            = mock[SystemClock]
  val user             = mock[User]
  val importService    = mock[ImportService]

  val ndlaClient       = mock[NdlaClient]
  val articleApiClient = mock[ArticleApiClient]
  val imageApiClient   = mock[ImageApiClient]

}
