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
import no.ndla.common.converter.CommonConverter
import no.ndla.common.{Clock, UUIDUtil}
import no.ndla.database.{DBMigrator, DBUtility, DataSource}
import no.ndla.draftapi.caching.MemoizeHelpers
import no.ndla.draftapi.controller.*
import no.ndla.draftapi.db.migrationwithdependencies.V57__MigrateSavedSearch
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
import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment
    extends TapirApplication
    with Elastic4sClient
    with ArticleSearchService
    with ArticleIndexService
    with TagSearchService
    with TagIndexService
    with GrepCodesSearchService
    with GrepCodesIndexService
    with IndexService
    with SearchLanguage
    with BaseIndexService
    with SearchService
    with StrictLogging
    with DraftController
    with InternController
    with UserDataController
    with ReindexClient
    with DataSource
    with TaxonomyApiClient
    with H5PApiClient
    with DraftRepository
    with UserDataRepository
    with MockitoSugar
    with CommonConverter
    with ConverterService
    with StateTransitionRules
    with LearningpathApiClient
    with NdlaClient
    with SearchConverterService
    with ReadService
    with WriteService
    with DBUtility
    with ContentValidator
    with FileController
    with FileStorageService
    with NdlaS3Client
    with Clock
    with UUIDUtil
    with ArticleApiClient
    with SearchApiClient
    with ErrorHandling
    with MemoizeHelpers
    with DBMigrator
    with Props
    with V57__MigrateSavedSearch
    with ImageApiClient {
  lazy val props: DraftApiProperties = new DraftApiProperties {
    override def InlineHtmlTags: Set[String]       = Set("code", "em", "span", "strong", "sub", "sup")
    override def IntroductionHtmlTags: Set[String] = InlineHtmlTags ++ Set("br", "p")
  }
  override lazy val migrator: DBMigrator = mock[DBMigrator]
  override lazy val DBUtil: DBUtility    = mock[DBUtility]

  override lazy val articleSearchService: ArticleSearchService     = mock[ArticleSearchService]
  override lazy val articleIndexService: ArticleIndexService       = mock[ArticleIndexService]
  override lazy val tagSearchService: TagSearchService             = mock[TagSearchService]
  override lazy val tagIndexService: TagIndexService               = mock[TagIndexService]
  override lazy val grepCodesSearchService: GrepCodesSearchService = mock[GrepCodesSearchService]
  override lazy val grepCodesIndexService: GrepCodesIndexService   = mock[GrepCodesIndexService]

  override lazy val internController: InternController      = mock[InternController]
  override lazy val draftController: DraftController        = mock[DraftController]
  override lazy val fileController: FileController          = mock[FileController]
  override lazy val userDataController: UserDataController  = mock[UserDataController]
  override lazy val healthController: TapirHealthController = mock[TapirHealthController]

  override lazy val dataSource: HikariDataSource           = mock[HikariDataSource]
  override lazy val draftRepository: DraftRepository       = mock[DraftRepository]
  override lazy val userDataRepository: UserDataRepository = mock[UserDataRepository]

  override lazy val converterService: ConverterService = mock[ConverterService]

  override lazy val readService: ReadService           = mock[ReadService]
  override lazy val writeService: WriteService         = mock[WriteService]
  override lazy val contentValidator: ContentValidator = mock[ContentValidator]
  override lazy val importValidator: ContentValidator  = mock[ContentValidator]
  override lazy val reindexClient: ReindexClient       = mock[ReindexClient]

  override lazy val fileStorage: FileStorageService = mock[FileStorageService]
  override lazy val s3Client: NdlaS3Client          = mock[NdlaS3Client]

  override lazy val ndlaClient: NdlaClient                         = mock[NdlaClient]
  override lazy val myndlaApiClient: MyNDLAApiClient               = mock[MyNDLAApiClient]
  override lazy val searchConverterService: SearchConverterService = mock[SearchConverterService]
  var e4sClient: NdlaE4sClient                                     = mock[NdlaE4sClient]
  override lazy val learningpathApiClient: LearningpathApiClient   = mock[LearningpathApiClient]

  override lazy val clock: SystemClock = mock[SystemClock]
  override lazy val uuidUtil: UUIDUtil = mock[UUIDUtil]

  override lazy val articleApiClient: ArticleApiClient   = mock[ArticleApiClient]
  override lazy val searchApiClient: SearchApiClient     = mock[SearchApiClient]
  override lazy val taxonomyApiClient: TaxonomyApiClient = mock[TaxonomyApiClient]
  override lazy val h5pApiClient: H5PApiClient           = mock[H5PApiClient]
  override lazy val imageApiClient: ImageApiClient       = mock[ImageApiClient]

  def services: List[TapirController] = List.empty
  val swagger: SwaggerController      = mock[SwaggerController]
}
