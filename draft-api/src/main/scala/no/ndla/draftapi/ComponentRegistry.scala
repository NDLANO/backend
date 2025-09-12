/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi

import no.ndla.common.aws.NdlaS3Client
import no.ndla.common.converter.CommonConverter
import no.ndla.common.util.TraitUtil
import no.ndla.common.{Clock, UUIDUtil}
import no.ndla.database.{DBMigrator, DBUtility, DataSource}
import no.ndla.draftapi.caching.MemoizeHelpers
import no.ndla.draftapi.controller.*
import no.ndla.draftapi.db.migrationwithdependencies.*
import no.ndla.draftapi.integration.*
import no.ndla.draftapi.model.api.DraftErrorHelpers
import no.ndla.draftapi.repository.{DraftRepository, UserDataRepository}
import no.ndla.draftapi.service.*
import no.ndla.draftapi.service.search.*
import no.ndla.draftapi.validation.ContentValidator
import no.ndla.network.NdlaClient
import no.ndla.network.clients.{MyNDLAApiClient, SearchApiClient, TaxonomyApiClient as BaseTaxonomyApiClient}
import no.ndla.network.tapir.*
import no.ndla.search.{Elastic4sClientFactory, NdlaE4sClient, SearchLanguage}

class ComponentRegistry(properties: DraftApiProperties) extends TapirApplication[DraftApiProperties] {
  implicit lazy val props: DraftApiProperties            = properties
  implicit lazy val dataSource: DataSource               = DataSource.getDataSource
  implicit lazy val errorHelpers: ErrorHelpers           = new ErrorHelpers
  implicit lazy val draftErrorHelpers: DraftErrorHelpers = new DraftErrorHelpers
  implicit lazy val errorHandling: ErrorHandling         = new ControllerErrorHandling

  implicit lazy val clock: Clock                               = new Clock
  implicit lazy val e4sClient: NdlaE4sClient                   = Elastic4sClientFactory.getClient(props.SearchServer)
  implicit lazy val searchLanguage: SearchLanguage             = new SearchLanguage
  implicit lazy val dbUtility: DBUtility                       = new DBUtility
  implicit lazy val memoizeHelpers: MemoizeHelpers             = new MemoizeHelpers
  implicit lazy val uuidUtil: UUIDUtil                         = new UUIDUtil
  implicit lazy val commonConverter: CommonConverter           = new CommonConverter
  implicit lazy val stateTransitionRules: StateTransitionRules = new StateTransitionRules
  implicit lazy val ndlaClient: NdlaClient                     = new NdlaClient
  implicit lazy val searchApiClient: SearchApiClient           = new SearchApiClient(props.SearchApiUrl)
  implicit lazy val myndlaApiClient: MyNDLAApiClient           = new MyNDLAApiClient
  implicit lazy val s3Client: NdlaS3Client                     =
    new NdlaS3Client(props.AttachmentStorageName, props.AttachmentStorageRegion)
  implicit lazy val articleApiClient: ArticleApiClient             = new ArticleApiClient
  implicit lazy val taxonomyApiClient: TaxonomyApiClient           = new TaxonomyApiClient
  implicit lazy val baseTaxonomyApiClient: BaseTaxonomyApiClient   = new BaseTaxonomyApiClient(props.TaxonomyUrl)
  implicit lazy val learningpathApiClient: LearningpathApiClient   = new LearningpathApiClient
  implicit lazy val h5pApiClient: H5PApiClient                     = new H5PApiClient
  implicit lazy val imageApiClient: ImageApiClient                 = new ImageApiClient
  implicit lazy val draftRepository: DraftRepository               = new DraftRepository
  implicit lazy val userDataRepository: UserDataRepository         = new UserDataRepository
  implicit lazy val contentValidator: ContentValidator             = new ContentValidator()
  implicit lazy val traitUtil: TraitUtil                           = new TraitUtil
  implicit lazy val converterService: ConverterService             = new ConverterService
  implicit lazy val searchConverterService: SearchConverterService = new SearchConverterService
  implicit lazy val readService: ReadService                       = new ReadService
  implicit lazy val writeService: WriteService                     = new WriteService
  implicit lazy val fileStorage: FileStorageService                = new FileStorageService
  implicit lazy val reindexClient: ReindexClient                   = new ReindexClient
  implicit lazy val articleSearchService: ArticleSearchService     = new ArticleSearchService
  implicit lazy val articleIndexService: ArticleIndexService       = new ArticleIndexService
  implicit lazy val tagSearchService: TagSearchService             = new TagSearchService
  implicit lazy val tagIndexService: TagIndexService               = new TagIndexService
  implicit lazy val grepCodesSearchService: GrepCodesSearchService = new GrepCodesSearchService
  implicit lazy val grepCodesIndexService: GrepCodesIndexService   = new GrepCodesIndexService
  implicit lazy val internController: InternController             = new InternController
  implicit lazy val draftController: DraftController               = new DraftController
  implicit lazy val fileController: FileController                 = new FileController
  implicit lazy val userDataController: UserDataController         = new UserDataController
  implicit lazy val healthController: TapirHealthController        = new TapirHealthController

  implicit lazy val migrator: DBMigrator = DBMigrator(
    new R__RemoveEmptyStringLanguageFields(props),
    new R__RemoveStatusPublishedArticles,
    new R__SetArticleLanguageFromTaxonomy,
    new R__SetArticleTypeFromTaxonomy,
    new V20__UpdateH5PDomainForFF,
    new V23__UpdateH5PDomainForFFVisualElement,
    new V33__ConvertLanguageUnknown,
    new V57__MigrateSavedSearch,
    new V66__SetHideBylineForImagesNotCopyrighted,
    new V76__ComputeSearchTraits,
    new V77__SetResourceTypeFromTaxonomyAsTag
  )

  implicit lazy val swagger: SwaggerController = new SwaggerController(
    List[TapirController](
      draftController,
      fileController,
      userDataController,
      internController,
      healthController
    ),
    SwaggerDocControllerConfig.swaggerInfo
  )

  implicit lazy val services: List[TapirController] = swagger.getServices()
  implicit lazy val routes: Routes                  = new Routes
}
