/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.typesafe.scalalogging.LazyLogging
import com.zaxxer.hikari.HikariDataSource
import no.ndla.draftapi.auth.User
import no.ndla.draftapi.controller._
import no.ndla.draftapi.integration._
import no.ndla.draftapi.repository.{AgreementRepository, DraftRepository, UserDataRepository}
import no.ndla.draftapi.service._
import no.ndla.draftapi.service.search._
import no.ndla.draftapi.validation.ContentValidator
import no.ndla.network.NdlaClient
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

object ComponentRegistry
    extends DataSource
    with InternController
    with ConverterService
    with StateTransitionRules
    with LearningpathApiClient
    with TaxonomyApiClient
    with DraftController
    with AgreementController
    with HealthController
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
    with ArticleIndexService
    with TagIndexService
    with GrepCodesIndexService
    with AgreementIndexService
    with SearchService
    with LazyLogging
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
    with UserDataController {

  def connectToDatabase(): Unit = ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

  implicit val swagger: DraftSwagger = new DraftSwagger

  override val dataSource: HikariDataSource = DataSource.getHikariDataSource
  connectToDatabase()

  lazy val internController = new InternController
  lazy val draftController = new DraftController
  lazy val fileController = new FileController
  lazy val agreementController = new AgreementController
  lazy val ruleController = new RuleController
  lazy val resourcesApp = new ResourcesApp
  lazy val healthController = new HealthController
  lazy val userDataController = new UserDataController

  lazy val draftRepository = new ArticleRepository
  lazy val agreementRepository = new AgreementRepository
  lazy val userDataRepository = new UserDataRepository

  lazy val articleSearchService = new ArticleSearchService
  lazy val articleIndexService = new ArticleIndexService
  lazy val tagSearchService = new TagSearchService
  lazy val tagIndexService = new TagIndexService
  lazy val grepCodesSearchService = new GrepCodesSearchService
  lazy val grepCodesIndexService = new GrepCodesIndexService
  lazy val agreementSearchService = new AgreementSearchService
  lazy val agreementIndexService = new AgreementIndexService

  lazy val converterService = new ConverterService
  lazy val contentValidator = new ContentValidator()
  lazy val importValidator = new ContentValidator()

  lazy val ndlaClient = new NdlaClient
  lazy val searchConverterService = new SearchConverterService
  lazy val readService = new ReadService
  lazy val writeService = new WriteService
  lazy val reindexClient = new ReindexClient

  lazy val fileStorage = new FileStorageService
  val currentRegion: Option[Regions] = Option(Regions.getCurrentRegion).map(region => Regions.fromName(region.getName))

  val amazonClient: AmazonS3 =
    AmazonS3ClientBuilder
      .standard()
      .withRegion(currentRegion.getOrElse(Regions.EU_WEST_1))
      .build()

  var e4sClient: NdlaE4sClient = Elastic4sClientFactory.getClient()

  lazy val clock = new SystemClock

  lazy val articleApiClient = new ArticleApiClient
  lazy val searchApiClient = new SearchApiClient
  lazy val taxonomyApiClient = new TaxonomyApiClient
  lazy val learningpathApiClient = new LearningpathApiClient
  lazy val conceptApiClient = new ConceptApiClient
  lazy val h5pApiClient = new H5PApiClient
  lazy val user = new User
}
