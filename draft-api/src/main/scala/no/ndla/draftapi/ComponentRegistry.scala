/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi

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
import no.ndla.draftapi.repository.{DraftRepository, UserDataRepository}
import no.ndla.draftapi.service.*
import no.ndla.draftapi.service.search.*
import no.ndla.draftapi.validation.ContentValidator
import no.ndla.common.{Clock, UUIDUtil}
import no.ndla.common.aws.NdlaS3Client
import no.ndla.database.{DBMigrator, DBUtility, DataSource}
import no.ndla.draftapi.model.api.DraftErrorHelpers
import no.ndla.network.NdlaClient
import no.ndla.network.tapir.{
  ErrorHandling,
  ErrorHelpers,
  Routes,
  SwaggerController,
  TapirApplication,
  TapirController,
  TapirHealthController
}
import no.ndla.network.clients.{MyNDLAApiClient, SearchApiClient}
import no.ndla.search.{Elastic4sClientFactory, NdlaE4sClient, SearchLanguage}
import no.ndla.common.converter.CommonConverter

class ComponentRegistry(properties: DraftApiProperties) extends TapirApplication[DraftApiProperties] {
  implicit val props: DraftApiProperties            = properties
  implicit val dataSource: DataSource               = DataSource.getDataSource
  implicit val errorHelpers: ErrorHelpers           = new ErrorHelpers
  implicit val draftErrorHelpers: DraftErrorHelpers = new DraftErrorHelpers
  implicit val errorHandling: ErrorHandling         = new ControllerErrorHandling

  implicit val migrator: DBMigrator = DBMigrator(
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

  implicit val clock: Clock                               = new Clock
  implicit val e4sClient: NdlaE4sClient                   = Elastic4sClientFactory.getClient(props.SearchServer)
  implicit val searchLanguage: SearchLanguage             = new SearchLanguage
  implicit val dbUtility: DBUtility                       = new DBUtility
  implicit val memoizeHelpers: MemoizeHelpers             = new MemoizeHelpers
  implicit val uuidUtil: UUIDUtil                         = new UUIDUtil
  implicit val commonConverter: CommonConverter           = new CommonConverter
  implicit val stateTransitionRules: StateTransitionRules = new StateTransitionRules
  implicit val ndlaClient: NdlaClient           = new NdlaClient
  implicit val searchApiClient: SearchApiClient = new SearchApiClient(props.SearchApiUrl)
  implicit val myndlaApiClient: MyNDLAApiClient = new MyNDLAApiClient
  implicit val s3Client: NdlaS3Client = new NdlaS3Client(props.AttachmentStorageName, props.AttachmentStorageRegion)
  implicit val articleApiClient: ArticleApiClient           = new ArticleApiClient
  implicit val taxonomyApiClient: TaxonomyApiClient         = new TaxonomyApiClient
  implicit val learningpathApiClient: LearningpathApiClient = new LearningpathApiClient
  implicit val h5pApiClient: H5PApiClient                   = new H5PApiClient
  implicit val imageApiClient: ImageApiClient               = new ImageApiClient
  implicit val draftRepository: DraftRepository       = new DraftRepository
  implicit val userDataRepository: UserDataRepository = new UserDataRepository
  implicit val contentValidator: ContentValidator             = new ContentValidator()
  implicit val converterService: ConverterService             = new ConverterService
  implicit val searchConverterService: SearchConverterService = new SearchConverterService
  implicit val readService: ReadService                       = new ReadService
  implicit val writeService: WriteService                     = new WriteService
  implicit val fileStorage: FileStorageService                = new FileStorageService
  implicit val reindexClient: ReindexClient                   = new ReindexClient
  implicit val articleSearchService: ArticleSearchService     = new ArticleSearchService
  implicit val articleIndexService: ArticleIndexService       = new ArticleIndexService
  implicit val tagSearchService: TagSearchService             = new TagSearchService
  implicit val tagIndexService: TagIndexService               = new TagIndexService
  implicit val grepCodesSearchService: GrepCodesSearchService = new GrepCodesSearchService
  implicit val grepCodesIndexService: GrepCodesIndexService   = new GrepCodesIndexService
  implicit val internController: InternController      = new InternController
  implicit val draftController: DraftController        = new DraftController
  implicit val fileController: FileController          = new FileController
  implicit val userDataController: UserDataController  = new UserDataController
  implicit val healthController: TapirHealthController = new TapirHealthController

  implicit val swagger: SwaggerController = new SwaggerController(
    List[TapirController](
      draftController,
      fileController,
      userDataController,
      internController,
      healthController
    ),
    SwaggerDocControllerConfig.swaggerInfo
  )

  implicit val services: List[TapirController] = swagger.getServices()
  implicit val routes: Routes                  = new Routes
}
