/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi

import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.typesafe.scalalogging.StrictLogging
import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.{Clock, UUIDUtil}
import no.ndla.common.configuration.BaseComponentRegistry
import no.ndla.draftapi.caching.MemoizeHelpers
import no.ndla.draftapi.controller._
import no.ndla.draftapi.integration._
import no.ndla.draftapi.model.api.ErrorHelpers
import no.ndla.draftapi.model.domain.DBArticle
import no.ndla.draftapi.repository.{DraftRepository, UserDataRepository}
import no.ndla.draftapi.service._
import no.ndla.draftapi.service.search._
import no.ndla.draftapi.validation.ContentValidator
import no.ndla.network.NdlaClient
import no.ndla.network.tapir.{
  NdlaMiddleware,
  Routes,
  Service,
  SwaggerControllerConfig,
  TapirErrorHelpers,
  TapirHealthController
}
import no.ndla.search.{BaseIndexService, Elastic4sClient}

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
    with TapirHealthController
    with MemoizeHelpers
    with DraftRepository
    with UserDataRepository
    with Elastic4sClient
    with ReindexClient
    with ArticleSearchService
    with TagSearchService
    with GrepCodesSearchService
    with IndexService
    with BaseIndexService
    with ArticleIndexService
    with TagIndexService
    with GrepCodesIndexService
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
    with UUIDUtil
    with ArticleApiClient
    with SearchApiClient
    with H5PApiClient
    with RuleController
    with UserDataController
    with Props
    with DBMigrator
    with ErrorHelpers
    with Routes[Eff]
    with NdlaMiddleware
    with TapirErrorHelpers
    with SwaggerControllerConfig
    with SwaggerDocControllerConfig {
  override val props: DraftApiProperties = properties

  override val migrator                     = new DBMigrator
  override val dataSource: HikariDataSource = DataSource.getHikariDataSource
  DataSource.connectToDatabase()

  lazy val draftRepository    = new ArticleRepository
  lazy val userDataRepository = new UserDataRepository

  lazy val articleSearchService   = new ArticleSearchService
  lazy val articleIndexService    = new ArticleIndexService
  lazy val tagSearchService       = new TagSearchService
  lazy val tagIndexService        = new TagIndexService
  lazy val grepCodesSearchService = new GrepCodesSearchService
  lazy val grepCodesIndexService  = new GrepCodesIndexService

  lazy val converterService = new ConverterService
  lazy val contentValidator = new ContentValidator()
  lazy val importValidator  = new ContentValidator()

  lazy val ndlaClient             = new NdlaClient
  lazy val searchConverterService = new SearchConverterService
  lazy val readService            = new ReadService
  lazy val writeService           = new WriteService
  lazy val reindexClient          = new ReindexClient

  lazy val fileStorage = new FileStorageService

  val amazonClient: AmazonS3 =
    AmazonS3ClientBuilder
      .standard()
      .withRegion(props.AttachmentStorageRegion)
      .build()

  var e4sClient: NdlaE4sClient = Elastic4sClientFactory.getClient(props.SearchServer)

  lazy val clock    = new SystemClock
  lazy val uuidUtil = new UUIDUtil

  lazy val articleApiClient      = new ArticleApiClient
  lazy val searchApiClient       = new SearchApiClient
  lazy val taxonomyApiClient     = new TaxonomyApiClient
  lazy val learningpathApiClient = new LearningpathApiClient
  lazy val h5pApiClient          = new H5PApiClient

  lazy val internController                             = new InternController
  lazy val draftController                              = new DraftController
  lazy val fileController                               = new FileController
  lazy val ruleController                               = new RuleController
  lazy val userDataController                           = new UserDataController
  lazy val healthController: TapirHealthController[Eff] = new TapirHealthController[Eff]

  private val swagger = new SwaggerController(
    List[Service[Eff]](
      draftController,
      ruleController,
      userDataController,
      internController,
      healthController
    ),
    SwaggerDocControllerConfig.swaggerInfo
  )
  override def services: List[Service[Eff]] = swagger.getServices()
}
