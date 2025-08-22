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
import no.ndla.common.converter.CommonConverter
import no.ndla.database.{DBMigrator, DBUtility, DataSource}
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
import no.ndla.search.{BaseIndexService, Elastic4sClient, SearchLanguage}

class ComponentRegistry(properties: DraftApiProperties) extends TapirApplication[DraftApiProperties] {
  given props: DraftApiProperties = properties
  given migrator: DBMigrator      = DBMigrator(
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
  given dataSource: HikariDataSource = DataSource.getDataSource
  given DBUtil: DBUtility            = new DBUtility

  given draftRepository    = new DraftRepository
  given userDataRepository = new UserDataRepository

  given articleSearchService   = new ArticleSearchService
  given articleIndexService    = new ArticleIndexService
  given tagSearchService       = new TagSearchService
  given tagIndexService        = new TagIndexService
  given grepCodesSearchService = new GrepCodesSearchService
  given grepCodesIndexService  = new GrepCodesIndexService

  given converterService = new ConverterService
  given contentValidator = new ContentValidator()
  given importValidator  = new ContentValidator()

  given ndlaClient                       = new NdlaClient
  given myndlaApiClient: MyNDLAApiClient = new MyNDLAApiClient
  given searchConverterService           = new SearchConverterService
  given readService                      = new ReadService
  given writeService                     = new WriteService
  given reindexClient                    = new ReindexClient

  given fileStorage = new FileStorageService

  given s3Client = new NdlaS3Client(props.AttachmentStorageName, props.AttachmentStorageRegion)

  var e4sClient: NdlaE4sClient = Elastic4sClientFactory.getClient(props.SearchServer)

  given clock    = new SystemClock
  given uuidUtil = new UUIDUtil

  given articleApiClient      = new ArticleApiClient
  given searchApiClient       = new SearchApiClient
  given taxonomyApiClient     = new TaxonomyApiClient
  given learningpathApiClient = new LearningpathApiClient
  given h5pApiClient          = new H5PApiClient
  given imageApiClient        = new ImageApiClient

  given internController                        = new InternController
  given draftController                         = new DraftController
  given fileController                          = new FileController
  given userDataController                      = new UserDataController
  given healthController: TapirHealthController = new TapirHealthController

  val swagger = new SwaggerController(
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
