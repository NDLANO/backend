/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi

import com.amazonaws.services.s3.AmazonS3
import com.typesafe.scalalogging.StrictLogging
import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.Clock
import no.ndla.draftapi.auth.User
import no.ndla.draftapi.caching.MemoizeHelpers
import no.ndla.draftapi.controller._
import no.ndla.draftapi.integration._
import no.ndla.draftapi.model.api.ErrorHelpers
import no.ndla.draftapi.model.domain.DBArticle
import no.ndla.draftapi.repository.{AgreementRepository, DraftRepository, UserDataRepository}
import no.ndla.draftapi.service._
import no.ndla.draftapi.service.search._
import no.ndla.draftapi.validation.ContentValidator
import no.ndla.network.NdlaClient
import no.ndla.network.scalatra.{NdlaControllerBase, NdlaSwaggerSupport}
import no.ndla.search.{BaseIndexService, Elastic4sClient, NdlaE4sClient}
import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment
    extends Elastic4sClient
    with ArticleSearchService
    with ArticleIndexService
    with TagSearchService
    with TagIndexService
    with GrepCodesSearchService
    with GrepCodesIndexService
    with AgreementSearchService
    with AgreementIndexService
    with IndexService
    with BaseIndexService
    with SearchService
    with StrictLogging
    with DraftController
    with InternController
    with HealthController
    with AgreementController
    with UserDataController
    with ReindexClient
    with DataSource
    with TaxonomyApiClient
    with H5PApiClient
    with DraftRepository
    with AgreementRepository
    with UserDataRepository
    with MockitoSugar
    with ConverterService
    with StateTransitionRules
    with ConceptApiClient
    with LearningpathApiClient
    with NdlaClient
    with SearchConverterService
    with ReadService
    with WriteService
    with ContentValidator
    with FileController
    with FileStorageService
    with AmazonClient
    with Clock
    with User
    with ArticleApiClient
    with SearchApiClient
    with DBArticle
    with ErrorHelpers
    with MemoizeHelpers
    with NdlaController
    with NdlaControllerBase
    with NdlaSwaggerSupport
    with DBMigrator
    with Props
    with DraftApiInfo {
  val props: DraftApiProperties = new DraftApiProperties
  val migrator: DBMigrator      = mock[DBMigrator]

  val articleSearchService: ArticleSearchService   = mock[ArticleSearchService]
  val articleIndexService: ArticleIndexService    = mock[ArticleIndexService]
  val tagSearchService: TagSearchService       = mock[TagSearchService]
  val tagIndexService: TagIndexService        = mock[TagIndexService]
  val grepCodesSearchService: GrepCodesSearchService = mock[GrepCodesSearchService]
  val grepCodesIndexService: GrepCodesIndexService  = mock[GrepCodesIndexService]
  val agreementSearchService: AgreementSearchService = mock[AgreementSearchService]
  val agreementIndexService: AgreementIndexService  = mock[AgreementIndexService]

  val internController: InternController    = mock[InternController]
  val draftController: DraftController     = mock[DraftController]
  val fileController: FileController      = mock[FileController]
  val agreementController: AgreementController = mock[AgreementController]
  val userDataController: UserDataController  = mock[UserDataController]

  val healthController: HealthController = mock[HealthController]

  val dataSource: HikariDataSource          = mock[HikariDataSource]
  val draftRepository: ArticleRepository     = mock[ArticleRepository]
  val agreementRepository: AgreementRepository = mock[AgreementRepository]
  val userDataRepository: UserDataRepository  = mock[UserDataRepository]

  val converterService: ConverterService = mock[ConverterService]

  val readService: ReadService      = mock[ReadService]
  val writeService: WriteService     = mock[WriteService]
  val contentValidator: ContentValidator = mock[ContentValidator]
  val importValidator: ContentValidator  = mock[ContentValidator]
  val reindexClient: ReindexClient    = mock[ReindexClient]

  lazy val fileStorage: FileStorageService       = mock[FileStorageService]
  val amazonClient: AmazonS3 = mock[AmazonS3]

  val ndlaClient: NdlaClient                                            = mock[NdlaClient]
  val searchConverterService: SearchConverterService                                = mock[SearchConverterService]
  var e4sClient: NdlaE4sClient                                             = mock[NdlaE4sClient]
  override val learningpathApiClient: LearningpathApiClient = mock[LearningpathApiClient]

  val clock: SystemClock = mock[SystemClock]

  val articleApiClient: ArticleApiClient  = mock[ArticleApiClient]
  val searchApiClient: SearchApiClient   = mock[SearchApiClient]
  val taxonomyApiClient: TaxonomyApiClient = mock[TaxonomyApiClient]
  val conceptApiClient: ConceptApiClient  = mock[ConceptApiClient]
  val h5pApiClient: H5PApiClient      = mock[H5PApiClient]
  val user: User              = mock[User]
}
