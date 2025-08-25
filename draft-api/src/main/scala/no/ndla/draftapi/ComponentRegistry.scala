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
  given props: DraftApiProperties            = properties
  given dataSource: DataSource               = DataSource.getDataSource
  given errorHelpers: ErrorHelpers           = new ErrorHelpers
  given draftErrorHelpers: DraftErrorHelpers = new DraftErrorHelpers
  given errorHandling: ErrorHandling         = new ControllerErrorHandling

  given migrator: DBMigrator = DBMigrator(
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

  given clock: Clock                               = new Clock
  given e4sClient: NdlaE4sClient                   = Elastic4sClientFactory.getClient(props.SearchServer)
  given searchLanguage: SearchLanguage             = new SearchLanguage
  given dbUtility: DBUtility                       = new DBUtility
  given memoizeHelpers: MemoizeHelpers             = new MemoizeHelpers
  given uuidUtil: UUIDUtil                         = new UUIDUtil
  given commonConverter: CommonConverter           = new CommonConverter
  given stateTransitionRules: StateTransitionRules = new StateTransitionRules

  // Infrastructure clients
  given ndlaClient: NdlaClient           = new NdlaClient
  given searchApiClient: SearchApiClient = new SearchApiClient(props.SearchApiUrl)
  given myndlaApiClient: MyNDLAApiClient = new MyNDLAApiClient
  given s3Client: NdlaS3Client           = new NdlaS3Client(props.AttachmentStorageName, props.AttachmentStorageRegion)

  // Integration clients
  given articleApiClient: ArticleApiClient           = new ArticleApiClient
  given taxonomyApiClient: TaxonomyApiClient         = new TaxonomyApiClient
  given learningpathApiClient: LearningpathApiClient = new LearningpathApiClient
  given h5pApiClient: H5PApiClient                   = new H5PApiClient
  given imageApiClient: ImageApiClient               = new ImageApiClient

  // Repository
  given draftRepository: DraftRepository       = new DraftRepository
  given userDataRepository: UserDataRepository = new UserDataRepository

  // Services
  given contentValidator: ContentValidator             = new ContentValidator()
  val importValidator: ContentValidator                = new ContentValidator()
  given converterService: ConverterService             = new ConverterService
  given searchConverterService: SearchConverterService = new SearchConverterService
  given readService: ReadService                       = new ReadService
  given writeService: WriteService                     = new WriteService
  given fileStorage: FileStorageService                = new FileStorageService
  given reindexClient: ReindexClient                   = new ReindexClient

  // Search services
  given articleSearchService: ArticleSearchService     = new ArticleSearchService
  given articleIndexService: ArticleIndexService       = new ArticleIndexService
  given tagSearchService: TagSearchService             = new TagSearchService
  given tagIndexService: TagIndexService               = new TagIndexService
  given grepCodesSearchService: GrepCodesSearchService = new GrepCodesSearchService
  given grepCodesIndexService: GrepCodesIndexService   = new GrepCodesIndexService

  // Controllers
  given internController: InternController = new InternController(using
    readService,
    writeService,
    converterService,
    draftRepository,
    articleIndexService,
    tagIndexService,
    grepCodesIndexService,
    articleApiClient,
    props,
    errorHandling,
    errorHelpers,
    clock,
    myndlaApiClient
  )
  given draftController: DraftController = new DraftController(using
    readService,
    writeService,
    articleSearchService,
    searchConverterService,
    converterService,
    contentValidator,
    props,
    errorHandling,
    errorHelpers,
    clock,
    myndlaApiClient
  )
  given fileController: FileController =
    new FileController(using writeService, props, errorHandling, errorHelpers, clock, myndlaApiClient)
  given userDataController: UserDataController =
    new UserDataController(using readService, writeService, errorHandling, errorHelpers, clock, myndlaApiClient, props)
  given healthController: TapirHealthController = new TapirHealthController

  given swagger: SwaggerController = new SwaggerController(
    List[TapirController](
      draftController,
      fileController,
      userDataController,
      internController,
      healthController
    ),
    SwaggerDocControllerConfig.swaggerInfo
  )

  given services: List[TapirController] = swagger.getServices()
  given routes: Routes                  = new Routes
}
