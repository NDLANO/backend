/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi

import com.amazonaws.services.s3.AmazonS3
import com.typesafe.scalalogging.LazyLogging
import com.zaxxer.hikari.HikariDataSource
import no.ndla.draftapi.auth.User
import no.ndla.draftapi.controller._
import no.ndla.draftapi.integration._
import no.ndla.draftapi.repository.{AgreementRepository, DraftRepository, UserDataRepository}
import no.ndla.draftapi.service._
import no.ndla.draftapi.service.search._
import no.ndla.draftapi.validation.ContentValidator
import no.ndla.network.NdlaClient
import org.mockito.scalatest.MockitoSugar

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
    with SearchService
    with LazyLogging
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
    with SearchApiClient {
  val articleSearchService = mock[ArticleSearchService]
  val articleIndexService = mock[ArticleIndexService]
  val tagSearchService = mock[TagSearchService]
  val tagIndexService = mock[TagIndexService]
  val grepCodesSearchService = mock[GrepCodesSearchService]
  val grepCodesIndexService = mock[GrepCodesIndexService]
  val agreementSearchService = mock[AgreementSearchService]
  val agreementIndexService = mock[AgreementIndexService]

  val internController = mock[InternController]
  val draftController = mock[DraftController]
  val fileController = mock[FileController]
  val agreementController = mock[AgreementController]
  val userDataController = mock[UserDataController]

  val healthController = mock[HealthController]

  val dataSource = mock[HikariDataSource]
  val draftRepository = mock[ArticleRepository]
  val agreementRepository = mock[AgreementRepository]
  val userDataRepository = mock[UserDataRepository]

  val converterService = mock[ConverterService]

  val readService = mock[ReadService]
  val writeService = mock[WriteService]
  val contentValidator = mock[ContentValidator]
  val importValidator = mock[ContentValidator]
  val reindexClient = mock[ReindexClient]

  lazy val fileStorage = mock[FileStorageService]
  val amazonClient: AmazonS3 = mock[AmazonS3]

  val ndlaClient = mock[NdlaClient]
  val searchConverterService = mock[SearchConverterService]
  var e4sClient = mock[NdlaE4sClient]
  override val learningpathApiClient: LearningpathApiClient = mock[LearningpathApiClient]

  val clock = mock[SystemClock]

  val articleApiClient = mock[ArticleApiClient]
  val searchApiClient = mock[SearchApiClient]
  val taxonomyApiClient = mock[TaxonomyApiClient]
  val conceptApiClient = mock[ConceptApiClient]
  val h5pApiClient = mock[H5PApiClient]
  val user = mock[User]
}
