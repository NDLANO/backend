/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi

import com.typesafe.scalalogging.StrictLogging
import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.aws.NdlaS3Client
import no.ndla.common.{Clock, UUIDUtil}
import no.ndla.common.configuration.BaseComponentRegistry
import no.ndla.database.{DBMigrator, DataSource}
import no.ndla.draftapi.caching.MemoizeHelpers
import no.ndla.draftapi.controller.*
import no.ndla.draftapi.db.migrationwithdependencies.{
  R__RemoveEmptyStringLanguageFields,
  R__RemoveStatusPublishedArticles,
  R__SetArticleLanguageFromTaxonomy,
  R__SetArticleTypeFromTaxonomy,
  V20__UpdateH5PDomainForFF,
  V23__UpdateH5PDomainForFFVisualElement,
  V33__ConvertLanguageUnknown,
  V57__MigrateSavedSearch,
  V66__SetHideBylineForImagesNotCopyrighted
}
import no.ndla.draftapi.integration.*
import no.ndla.draftapi.model.api.ErrorHandling
import no.ndla.draftapi.repository.{DraftRepository, UserDataRepository}
import no.ndla.draftapi.service.*
import no.ndla.draftapi.service.search.*
import no.ndla.draftapi.validation.ContentValidator
import no.ndla.network.NdlaClient
import no.ndla.network.clients.SearchApiClient
import no.ndla.network.tapir.TapirApplication
import no.ndla.search.{BaseIndexService, Elastic4sClient}

class ComponentRegistry(properties: DraftApiProperties)
    extends BaseComponentRegistry[DraftApiProperties]
    with TapirApplication
    with DataSource
    with InternController
    with ConverterService
    with StateTransitionRules
    with LearningpathApiClient
    with TaxonomyApiClient
    with DraftController
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
    with NdlaS3Client
    with ContentValidator
    with Clock
    with UUIDUtil
    with ArticleApiClient
    with SearchApiClient
    with H5PApiClient
    with ImageApiClient
    with UserDataController
    with Props
    with DBMigrator
    with ErrorHandling
    with SwaggerDocControllerConfig
    with V57__MigrateSavedSearch
    with V66__SetHideBylineForImagesNotCopyrighted {
  override val props: DraftApiProperties = properties
  override val migrator: DBMigrator = DBMigrator(
    new R__RemoveEmptyStringLanguageFields(props),
    new R__RemoveStatusPublishedArticles(props),
    new R__SetArticleLanguageFromTaxonomy(props),
    new R__SetArticleTypeFromTaxonomy(props),
    new V20__UpdateH5PDomainForFF,
    new V23__UpdateH5PDomainForFFVisualElement,
    new V33__ConvertLanguageUnknown(props),
    new V57__MigrateSavedSearch,
    new V66__SetHideBylineForImagesNotCopyrighted
  )
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

  lazy val ndlaClient                       = new NdlaClient
  lazy val myndlaApiClient: MyNDLAApiClient = new MyNDLAApiClient
  lazy val searchConverterService           = new SearchConverterService
  lazy val readService                      = new ReadService
  lazy val writeService                     = new WriteService
  lazy val reindexClient                    = new ReindexClient

  lazy val fileStorage = new FileStorageService

  lazy val s3Client = new NdlaS3Client(props.AttachmentStorageName, props.AttachmentStorageRegion)

  var e4sClient: NdlaE4sClient = Elastic4sClientFactory.getClient(props.SearchServer)

  lazy val clock    = new SystemClock
  lazy val uuidUtil = new UUIDUtil

  lazy val articleApiClient      = new ArticleApiClient
  lazy val searchApiClient       = new SearchApiClient
  lazy val taxonomyApiClient     = new TaxonomyApiClient
  lazy val learningpathApiClient = new LearningpathApiClient
  lazy val h5pApiClient          = new H5PApiClient
  lazy val imageApiClient        = new ImageApiClient

  lazy val internController                        = new InternController
  lazy val draftController                         = new DraftController
  lazy val fileController                          = new FileController
  lazy val userDataController                      = new UserDataController
  lazy val healthController: TapirHealthController = new TapirHealthController

  private val swagger = new SwaggerController(
    List[TapirController](
      draftController,
      fileController,
      userDataController,
      internController,
      healthController
    ),
    SwaggerDocControllerConfig.swaggerInfo
  )
  override def services: List[TapirController] = swagger.getServices()
}
