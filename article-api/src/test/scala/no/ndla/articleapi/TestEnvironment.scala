/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import com.zaxxer.hikari.HikariDataSource
import no.ndla.articleapi.caching.MemoizeHelpers
import no.ndla.articleapi.controller._
import no.ndla.articleapi.integration._
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service._
import no.ndla.articleapi.service.search._
import no.ndla.articleapi.validation.ContentValidator
import no.ndla.articleapi.integration.SearchApiClient
import no.ndla.articleapi.model.api.ErrorHelpers
import no.ndla.articleapi.model.domain.DBArticle
import no.ndla.common.Clock
import no.ndla.network.NdlaClient
import no.ndla.network.clients.{FeideApiClient, RedisClient}
import no.ndla.network.tapir.{NdlaMiddleware, Routes, Service, TapirHealthController}
import no.ndla.search.{BaseIndexService, Elastic4sClient}
import org.mockito.scalatest.MockitoSugar

trait TestEnvironment
    extends Elastic4sClient
    with ArticleSearchService
    with ArticleIndexService
    with IndexService
    with BaseIndexService
    with SearchService
    with ArticleControllerV2
    with InternController
    with NdlaMiddleware
    with DataSource
    with Routes[Eff]
    with ArticleRepository
    with MockitoSugar
    with SearchApiClient
    with FeideApiClient
    with RedisClient
    with ConverterService
    with NdlaClient
    with SearchConverterService
    with ReadService
    with TapirHealthController
    with WriteService
    with ContentValidator
    with Clock
    with ErrorHelpers
    with MemoizeHelpers
    with DBArticle
    with Props
    with TestData
    with DBMigrator {
  val props: ArticleApiProperties = new ArticleApiProperties {
    override def InlineHtmlTags: Set[String]       = Set("code", "em", "span", "strong", "sub", "sup")
    override def IntroductionHtmlTags: Set[String] = InlineHtmlTags ++ Set("br", "p")
  }
  val TestData: TestData = new TestData
  val migrator           = mock[DBMigrator]

  val articleSearchService = mock[ArticleSearchService]
  val articleIndexService  = mock[ArticleIndexService]

  val internController    = mock[InternController]
  val articleControllerV2 = mock[ArticleControllerV2]

  val healthController = mock[TapirHealthController[Eff]]

  val dataSource        = mock[HikariDataSource]
  val articleRepository = mock[ArticleRepository]

  val converterService = mock[ConverterService]
  val readService      = mock[ReadService]
  val writeService     = mock[WriteService]
  val contentValidator = mock[ContentValidator]

  val ndlaClient             = mock[NdlaClient]
  val searchConverterService = mock[SearchConverterService]
  var e4sClient              = mock[NdlaE4sClient]
  val searchApiClient        = mock[SearchApiClient]
  val feideApiClient         = mock[FeideApiClient]
  val redisClient            = mock[RedisClient]

  val clock = mock[SystemClock]

  val services: List[Service[Eff]] = List.empty
}
