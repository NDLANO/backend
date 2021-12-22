/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import com.typesafe.scalalogging.LazyLogging
import com.zaxxer.hikari.HikariDataSource
import no.ndla.articleapi.auth.{Role, User}
import no.ndla.articleapi.controller._
import no.ndla.articleapi.integration._
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service._
import no.ndla.articleapi.service.search._
import no.ndla.articleapi.validation.ContentValidator
import no.ndla.articleapi.integration.SearchApiClient
import no.ndla.network.NdlaClient
import org.mockito.scalatest.MockitoSugar

trait TestEnvironment
    extends Elastic4sClient
    with ArticleSearchService
    with ArticleIndexService
    with IndexService
    with SearchService
    with LazyLogging
    with ArticleControllerV2
    with InternController
    with HealthController
    with DataSource
    with ArticleRepository
    with MockitoSugar
    with DraftApiClient
    with SearchApiClient
    with FeideApiClient
    with ConverterService
    with NdlaClient
    with SearchConverterService
    with ReadService
    with WriteService
    with ContentValidator
    with Clock
    with User
    with Role {

  val articleSearchService = mock[ArticleSearchService]
  val articleIndexService = mock[ArticleIndexService]

  val internController = mock[InternController]
  val articleControllerV2 = mock[ArticleControllerV2]

  val healthController = mock[HealthController]

  val dataSource = mock[HikariDataSource]
  val articleRepository = mock[ArticleRepository]

  val converterService = mock[ConverterService]
  val readService = mock[ReadService]
  val writeService = mock[WriteService]
  val contentValidator = mock[ContentValidator]

  val ndlaClient = mock[NdlaClient]
  val searchConverterService = mock[SearchConverterService]
  var e4sClient = mock[NdlaE4sClient]
  val draftApiClient = mock[DraftApiClient]
  val searchApiClient = mock[SearchApiClient]
  val feideApiClient = mock[FeideApiClient]

  val clock = mock[SystemClock]
  val authUser = mock[AuthUser]
  val authRole = new AuthRole
}
