/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi

import com.typesafe.scalalogging.StrictLogging
import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.aws.NdlaS3Client
import no.ndla.common.{Clock, UUIDUtil}
import no.ndla.draftapi.caching.MemoizeHelpers
import no.ndla.draftapi.controller.*
import no.ndla.draftapi.db.migrationwithdependencies.V57__MigrateSavedSearch
import no.ndla.draftapi.integration.*
import no.ndla.draftapi.model.api.ErrorHelpers
import no.ndla.draftapi.repository.{DraftRepository, UserDataRepository}
import no.ndla.draftapi.service.*
import no.ndla.draftapi.service.search.*
import no.ndla.draftapi.validation.ContentValidator
import no.ndla.network.NdlaClient
import no.ndla.network.tapir.{Routes, Service, TapirErrorHelpers}
import no.ndla.search.{BaseIndexService, Elastic4sClient}
import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment
    extends Elastic4sClient
    with ArticleSearchService
    with ArticleIndexService
    with TagSearchService
    with TagIndexService
    with GrepCodesSearchService
    with GrepCodesIndexService
    with IndexService
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
    with ConverterService
    with StateTransitionRules
    with LearningpathApiClient
    with NdlaClient
    with SearchConverterService
    with ReadService
    with WriteService
    with ContentValidator
    with FileController
    with FileStorageService
    with NdlaS3Client
    with Clock
    with UUIDUtil
    with ArticleApiClient
    with SearchApiClient
    with ErrorHelpers
    with MemoizeHelpers
    with DBMigrator
    with Props
    with Routes[Eff]
    with TapirErrorHelpers
    with V57__MigrateSavedSearch {
  val props: DraftApiProperties = new DraftApiProperties {
    override def InlineHtmlTags: Set[String]       = Set("code", "em", "span", "strong", "sub", "sup")
    override def IntroductionHtmlTags: Set[String] = InlineHtmlTags ++ Set("br", "p")
  }
  val migrator: DBMigrator = mock[DBMigrator]

  val articleSearchService: ArticleSearchService     = mock[ArticleSearchService]
  val articleIndexService: ArticleIndexService       = mock[ArticleIndexService]
  val tagSearchService: TagSearchService             = mock[TagSearchService]
  val tagIndexService: TagIndexService               = mock[TagIndexService]
  val grepCodesSearchService: GrepCodesSearchService = mock[GrepCodesSearchService]
  val grepCodesIndexService: GrepCodesIndexService   = mock[GrepCodesIndexService]

  val internController: InternController     = mock[InternController]
  val draftController: DraftController       = mock[DraftController]
  val fileController: FileController         = mock[FileController]
  val userDataController: UserDataController = mock[UserDataController]

  val dataSource: HikariDataSource           = mock[HikariDataSource]
  val draftRepository: ArticleRepository     = mock[ArticleRepository]
  val userDataRepository: UserDataRepository = mock[UserDataRepository]

  val converterService: ConverterService = mock[ConverterService]

  val readService: ReadService           = mock[ReadService]
  val writeService: WriteService         = mock[WriteService]
  val contentValidator: ContentValidator = mock[ContentValidator]
  val importValidator: ContentValidator  = mock[ContentValidator]
  val reindexClient: ReindexClient       = mock[ReindexClient]

  lazy val fileStorage: FileStorageService = mock[FileStorageService]
  val s3Client: NdlaS3Client               = mock[NdlaS3Client]

  val ndlaClient: NdlaClient                                = mock[NdlaClient]
  val searchConverterService: SearchConverterService        = mock[SearchConverterService]
  var e4sClient: NdlaE4sClient                              = mock[NdlaE4sClient]
  override val learningpathApiClient: LearningpathApiClient = mock[LearningpathApiClient]

  val clock: SystemClock = mock[SystemClock]
  val uuidUtil: UUIDUtil = mock[UUIDUtil]

  val articleApiClient: ArticleApiClient   = mock[ArticleApiClient]
  val searchApiClient: SearchApiClient     = mock[SearchApiClient]
  val taxonomyApiClient: TaxonomyApiClient = mock[TaxonomyApiClient]
  val h5pApiClient: H5PApiClient           = mock[H5PApiClient]

  def services: List[Service[Eff]] = List.empty
}
