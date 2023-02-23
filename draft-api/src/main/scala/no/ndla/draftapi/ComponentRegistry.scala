/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.typesafe.scalalogging.StrictLogging
import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.Clock
import no.ndla.common.configuration.BaseComponentRegistry
import no.ndla.draftapi.auth.User
import no.ndla.draftapi.caching.MemoizeHelpers
import no.ndla.draftapi.controller._
import no.ndla.draftapi.integration._
import no.ndla.draftapi.model.api.ErrorHelpers
import no.ndla.draftapi.model.domain.DBArticle
import no.ndla.draftapi.repository.{AgreementRepository, DraftRepository, UserDataRepository}
import no.ndla.draftapi.service._
import no.ndla.draftapi.service.search._
import no.ndla.draftapi.validation.ContentValidator
import no.ndla.network.NdlaClient
import no.ndla.network.scalatra.{NdlaControllerBase, NdlaSwaggerSupport}
import no.ndla.search.{BaseIndexService, Elastic4sClient, Elastic4sClientFactory, NdlaE4sClient}

class ComponentRegistry(properties: DraftApiProperties)
    extends BaseComponentRegistry[DraftApiProperties]
    with DataSource
    with InternController
    with DBArticle
    with ConverterService
    with StateTransitionRules
    with LearningpathApiClient
    with TaxonomyApiClient
    with DraftController
    with AgreementController
    with HealthController
    with NdlaController
    with NdlaControllerBase
    with NdlaSwaggerSupport
    with MemoizeHelpers
    with DraftRepository
    with AgreementRepository
    with UserDataRepository
    with Elastic4sClient
    with ReindexClient
    with ArticleSearchService
    with TagSearchService
    with GrepCodesSearchService
    with AgreementSearchService
    with IndexService
    with BaseIndexService
    with ArticleIndexService
    with TagIndexService
    with GrepCodesIndexService
    with AgreementIndexService
    with SearchService
    with StrictLogging
    with NdlaClient
    with SearchConverterService
    with ReadService
    with WriteService
    with FileController
    with FileStorageService
    with AmazonClient
    with ContentValidator
    with Clock
    with User
    with ArticleApiClient
    with SearchApiClient
    with ConceptApiClient
    with H5PApiClient
    with RuleController
    with UserDataController
    with Props
    with DBMigrator
    with ErrorHelpers
    with DraftApiInfo {
  override val props: DraftApiProperties = properties

  override val migrator                     = new DBMigrator
  override val dataSource: HikariDataSource = DataSource.getHikariDataSource
  DataSource.connectToDatabase()

  implicit val swagger: DraftSwagger = new DraftSwagger

  lazy val internController    = new InternController
  lazy val draftController     = new DraftController
  lazy val fileController      = new FileController
  lazy val agreementController = new AgreementController
  lazy val ruleController      = new RuleController
  lazy val resourcesApp        = new ResourcesApp
  lazy val healthController    = new HealthController
  lazy val userDataController  = new UserDataController

  lazy val draftRepository     = new ArticleRepository
  lazy val agreementRepository = new AgreementRepository
  lazy val userDataRepository  = new UserDataRepository

  lazy val articleSearchService   = new ArticleSearchService
  lazy val articleIndexService    = new ArticleIndexService
  lazy val tagSearchService       = new TagSearchService
  lazy val tagIndexService        = new TagIndexService
  lazy val grepCodesSearchService = new GrepCodesSearchService
  lazy val grepCodesIndexService  = new GrepCodesIndexService
  lazy val agreementSearchService = new AgreementSearchService
  lazy val agreementIndexService  = new AgreementIndexService

  lazy val converterService = new ConverterService
  lazy val contentValidator = new ContentValidator()
  lazy val importValidator  = new ContentValidator()

  lazy val ndlaClient             = new NdlaClient
  lazy val searchConverterService = new SearchConverterService
  lazy val readService            = new ReadService
  lazy val writeService           = new WriteService
  lazy val reindexClient          = new ReindexClient

  lazy val fileStorage               = new FileStorageService
  val currentRegion: Option[Regions] = Option(Regions.getCurrentRegion).map(region => Regions.fromName(region.getName))

  val amazonClient: AmazonS3 =
    AmazonS3ClientBuilder
      .standard()
      .withRegion(currentRegion.getOrElse(Regions.EU_WEST_1))
      .build()

  var e4sClient: NdlaE4sClient = Elastic4sClientFactory.getClient(props.SearchServer)

  lazy val clock = new SystemClock

  lazy val articleApiClient      = new ArticleApiClient
  lazy val searchApiClient       = new SearchApiClient
  lazy val taxonomyApiClient     = new TaxonomyApiClient
  lazy val learningpathApiClient = new LearningpathApiClient
  lazy val conceptApiClient      = new ConceptApiClient
  lazy val h5pApiClient          = new H5PApiClient
  lazy val user                  = new User
}
