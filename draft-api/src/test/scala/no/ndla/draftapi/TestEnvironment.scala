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
import no.ndla.common.{Clock, UUIDUtil}
import no.ndla.draftapi.caching.MemoizeHelpers
import no.ndla.draftapi.controller._
import no.ndla.draftapi.integration._
import no.ndla.draftapi.model.api.ErrorHelpers
import no.ndla.draftapi.model.domain.DBArticle
import no.ndla.draftapi.repository.{DraftRepository, UserDataRepository}
import no.ndla.draftapi.service._
import no.ndla.draftapi.service.search._
import no.ndla.draftapi.validation.ContentValidator
import no.ndla.network.NdlaClient
import no.ndla.network.scalatra.{NdlaControllerBase, NdlaSwaggerSupport}
import no.ndla.search.{BaseIndexService, Elastic4sClient}
import org.mockito.scalatest.MockitoSugar

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
    with HealthController
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
    with AmazonClient
    with Clock
    with UUIDUtil
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
  val props: DraftApiProperties = new DraftApiProperties {
    override def InlineHtmlTags: Set[String] = Set("code", "em", "span", "strong", "sub", "sup")
  }
  val migrator: DBMigrator = mock[DBMigrator]

  val articleSearchService   = mock[ArticleSearchService]
  val articleIndexService    = mock[ArticleIndexService]
  val tagSearchService       = mock[TagSearchService]
  val tagIndexService        = mock[TagIndexService]
  val grepCodesSearchService = mock[GrepCodesSearchService]
  val grepCodesIndexService  = mock[GrepCodesIndexService]

  val internController   = mock[InternController]
  val draftController    = mock[DraftController]
  val fileController     = mock[FileController]
  val userDataController = mock[UserDataController]

  val healthController = mock[HealthController]

  val dataSource         = mock[HikariDataSource]
  val draftRepository    = mock[ArticleRepository]
  val userDataRepository = mock[UserDataRepository]

  val converterService = mock[ConverterService]

  val readService      = mock[ReadService]
  val writeService     = mock[WriteService]
  val contentValidator = mock[ContentValidator]
  val importValidator  = mock[ContentValidator]
  val reindexClient    = mock[ReindexClient]

  lazy val fileStorage       = mock[FileStorageService]
  val amazonClient: AmazonS3 = mock[AmazonS3]

  val ndlaClient                                            = mock[NdlaClient]
  val searchConverterService                                = mock[SearchConverterService]
  var e4sClient                                             = mock[NdlaE4sClient]
  override val learningpathApiClient: LearningpathApiClient = mock[LearningpathApiClient]

  val clock    = mock[SystemClock]
  val uuidUtil = mock[UUIDUtil]

  val articleApiClient  = mock[ArticleApiClient]
  val searchApiClient   = mock[SearchApiClient]
  val taxonomyApiClient = mock[TaxonomyApiClient]
  val h5pApiClient      = mock[H5PApiClient]
}
