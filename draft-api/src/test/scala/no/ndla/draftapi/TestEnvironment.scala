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

trait TestEnvironment extends TapirApplication with MockitoSugar with StrictLogging {
  given props: DraftApiProperties = new DraftApiProperties {
    override def InlineHtmlTags: Set[String]       = Set("code", "em", "span", "strong", "sub", "sup")
    override def IntroductionHtmlTags: Set[String] = InlineHtmlTags ++ Set("br", "p")
  }
  given migrator: DBMigrator = mock[DBMigrator]
  given DBUtil: DBUtility    = mock[DBUtility]

  given articleSearchService: ArticleSearchService     = mock[ArticleSearchService]
  given articleIndexService: ArticleIndexService       = mock[ArticleIndexService]
  given tagSearchService: TagSearchService             = mock[TagSearchService]
  given tagIndexService: TagIndexService               = mock[TagIndexService]
  given grepCodesSearchService: GrepCodesSearchService = mock[GrepCodesSearchService]
  given grepCodesIndexService: GrepCodesIndexService   = mock[GrepCodesIndexService]

  given internController: InternController      = mock[InternController]
  given draftController: DraftController        = mock[DraftController]
  given fileController: FileController          = mock[FileController]
  given userDataController: UserDataController  = mock[UserDataController]
  given healthController: TapirHealthController = mock[TapirHealthController]

  given dataSource: HikariDataSource           = mock[HikariDataSource]
  given draftRepository: DraftRepository       = mock[DraftRepository]
  given userDataRepository: UserDataRepository = mock[UserDataRepository]

  given converterService: ConverterService = mock[ConverterService]

  given readService: ReadService           = mock[ReadService]
  given writeService: WriteService         = mock[WriteService]
  given contentValidator: ContentValidator = mock[ContentValidator]
  given importValidator: ContentValidator  = mock[ContentValidator]
  given reindexClient: ReindexClient       = mock[ReindexClient]

  given fileStorage: FileStorageService = mock[FileStorageService]
  given s3Client: NdlaS3Client          = mock[NdlaS3Client]

  given ndlaClient: NdlaClient                         = mock[NdlaClient]
  given myndlaApiClient: MyNDLAApiClient               = mock[MyNDLAApiClient]
  given searchConverterService: SearchConverterService = mock[SearchConverterService]
  given e4sClient: NdlaE4sClient                       = mock[NdlaE4sClient]
  given learningpathApiClient: LearningpathApiClient   = mock[LearningpathApiClient]

  given clock: SystemClock = mock[SystemClock]
  given uuidUtil: UUIDUtil = mock[UUIDUtil]

  given articleApiClient: ArticleApiClient   = mock[ArticleApiClient]
  given searchApiClient: SearchApiClient     = mock[SearchApiClient]
  given taxonomyApiClient: TaxonomyApiClient = mock[TaxonomyApiClient]
  given h5pApiClient: H5PApiClient           = mock[H5PApiClient]
  given imageApiClient: ImageApiClient       = mock[ImageApiClient]

  def services: List[TapirController] = List.empty
  val swagger: SwaggerController      = mock[SwaggerController]
}
