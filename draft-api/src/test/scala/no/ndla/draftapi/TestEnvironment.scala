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
import no.ndla.draftapi.integration.*
import no.ndla.draftapi.model.api.DraftErrorHelpers
import no.ndla.draftapi.model.domain.{DBArticle, DBUserData}
import no.ndla.draftapi.repository.{DraftRepository, UserDataRepository}
import no.ndla.draftapi.service.*
import no.ndla.draftapi.service.search.*
import no.ndla.draftapi.validation.ContentValidator
import no.ndla.network.NdlaClient
import no.ndla.network.clients.{MyNDLAApiClient, SearchApiClient, TaxonomyApiClient as BaseTaxonomyApiClient}
import no.ndla.network.tapir.*
import no.ndla.scalatestsuite.DBUtilityStub
import no.ndla.search.{NdlaE4sClient, SearchLanguage}
import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment extends TapirApplication[DraftApiProperties] with MockitoSugar {
  implicit lazy val props: DraftApiProperties = new DraftApiProperties {
    override def InlineHtmlTags: Set[String]       = Set("code", "em", "span", "strong", "sub", "sup")
    override def IntroductionHtmlTags: Set[String] = InlineHtmlTags ++ Set("br", "p")
  }

  implicit lazy val migrator: DBMigrator                 = mock[DBMigrator]
  implicit lazy val dbUtility: DBUtility                 = DBUtilityStub()
  implicit lazy val dbArticle: DBArticle                 = new DBArticle
  implicit lazy val dbUserData: DBUserData               = new DBUserData
  implicit lazy val uuidUtil: UUIDUtil                   = mock[UUIDUtil]
  implicit lazy val memoizeHelpers: MemoizeHelpers       = mock[MemoizeHelpers]
  implicit lazy val searchLanguage: SearchLanguage       = mock[SearchLanguage]
  implicit lazy val errorHelpers: ErrorHelpers           = new ErrorHelpers
  implicit lazy val draftErrorHelpers: DraftErrorHelpers = new DraftErrorHelpers
  implicit lazy val errorHandling: ErrorHandling         = new ControllerErrorHandling
  implicit lazy val clock: Clock                         = mock[Clock]
  implicit lazy val routes: Routes                       = mock[Routes]
  implicit lazy val services: List[TapirController]      = List.empty

  implicit lazy val articleSearchService: ArticleSearchService     = mock[ArticleSearchService]
  implicit lazy val articleIndexService: ArticleIndexService       = mock[ArticleIndexService]
  implicit lazy val tagSearchService: TagSearchService             = mock[TagSearchService]
  implicit lazy val tagIndexService: TagIndexService               = mock[TagIndexService]
  implicit lazy val grepCodesSearchService: GrepCodesSearchService = mock[GrepCodesSearchService]
  implicit lazy val grepCodesIndexService: GrepCodesIndexService   = mock[GrepCodesIndexService]
  implicit lazy val traitUtil: TraitUtil                           = mock[TraitUtil]

  implicit lazy val internController: InternController      = mock[InternController]
  implicit lazy val draftController: DraftController        = mock[DraftController]
  implicit lazy val fileController: FileController          = mock[FileController]
  implicit lazy val userDataController: UserDataController  = mock[UserDataController]
  implicit lazy val healthController: TapirHealthController = mock[TapirHealthController]

  implicit lazy val dataSource: DataSource                 = mock[DataSource]
  implicit lazy val draftRepository: DraftRepository       = mock[DraftRepository]
  implicit lazy val userDataRepository: UserDataRepository = mock[UserDataRepository]

  implicit lazy val converterService: ConverterService         = mock[ConverterService]
  implicit lazy val commonConverter: CommonConverter           = mock[CommonConverter]
  implicit lazy val stateTransitionRules: StateTransitionRules = mock[StateTransitionRules]

  implicit lazy val readService: ReadService           = mock[ReadService]
  implicit lazy val writeService: WriteService         = mock[WriteService]
  implicit lazy val contentValidator: ContentValidator = mock[ContentValidator]
  implicit lazy val reindexClient: ReindexClient       = mock[ReindexClient]

  implicit lazy val fileStorage: FileStorageService = mock[FileStorageService]
  implicit lazy val s3Client: NdlaS3Client          = mock[NdlaS3Client]

  implicit lazy val ndlaClient: NdlaClient                         = mock[NdlaClient]
  implicit lazy val myndlaApiClient: MyNDLAApiClient               = mock[MyNDLAApiClient]
  implicit lazy val searchConverterService: SearchConverterService = mock[SearchConverterService]
  implicit lazy val e4sClient: NdlaE4sClient                       = mock[NdlaE4sClient]
  implicit lazy val learningpathApiClient: LearningpathApiClient   = mock[LearningpathApiClient]

  implicit lazy val articleApiClient: ArticleApiClient           = mock[ArticleApiClient]
  implicit lazy val searchApiClient: SearchApiClient             = mock[SearchApiClient]
  implicit lazy val taxonomyApiClient: TaxonomyApiClient         = mock[TaxonomyApiClient]
  implicit lazy val baseTaxonomyApiClient: BaseTaxonomyApiClient = mock[BaseTaxonomyApiClient]
  implicit lazy val h5pApiClient: H5PApiClient                   = mock[H5PApiClient]
  implicit lazy val imageApiClient: ImageApiClient               = mock[ImageApiClient]

  implicit lazy val swagger: SwaggerController = mock[SwaggerController]
}
