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
import no.ndla.articleapi.caching.MemoizeHelpers
import no.ndla.articleapi.controller.{ArticleControllerV2, HealthController, InternController, NdlaController}
import no.ndla.articleapi.integration._
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service._
import no.ndla.articleapi.service.search._
import no.ndla.articleapi.validation.ContentValidator
import no.ndla.articleapi.integration.SearchApiClient
import no.ndla.articleapi.model.api.ErrorHelpers
import no.ndla.articleapi.model.domain.DBArticle
import no.ndla.common.Clock
import no.ndla.common.configuration.BaseComponentRegistry
import no.ndla.network.NdlaClient
import no.ndla.network.clients.FeideApiClient
import no.ndla.search.{BaseIndexService, Elastic4sClient, Elastic4sClientFactory, NdlaE4sClient}

class ComponentRegistry(properties: ArticleApiProperties)
    extends BaseComponentRegistry[ArticleApiProperties]
    with Props
    with DataSource
    with InternController
    with ArticleControllerV2
    with NdlaController
    with HealthController
    with ArticleRepository
    with Elastic4sClient
    with DraftApiClient
    with SearchApiClient
    with FeideApiClient
    with ArticleSearchService
    with IndexService
    with BaseIndexService
    with ArticleIndexService
    with SearchService
    with LazyLogging
    with ConverterService
    with NdlaClient
    with SearchConverterService
    with ReadService
    with MemoizeHelpers
    with WriteService
    with ContentValidator
    with Clock
    with Role
    with User
    with ArticleApiInfo
    with ErrorHelpers
    with DBArticle
    with DBMigrator {
  override val props: ArticleApiProperties = properties
  override val migrator                    = new DBMigrator

  implicit val swagger: ArticleSwagger = new ArticleSwagger

  override val dataSource: HikariDataSource = DataSource.getHikariDataSource
  DataSource.connectToDatabase()

  lazy val internController    = new InternController
  lazy val articleControllerV2 = new ArticleControllerV2
  lazy val resourcesApp        = new ResourcesApp
  lazy val healthController    = new HealthController

  lazy val articleRepository    = new ArticleRepository
  lazy val articleSearchService = new ArticleSearchService
  lazy val articleIndexService  = new ArticleIndexService

  lazy val converterService = new ConverterService
  lazy val contentValidator = new ContentValidator()

  lazy val ndlaClient             = new NdlaClient
  lazy val searchConverterService = new SearchConverterService
  lazy val readService            = new ReadService
  lazy val writeService           = new WriteService

  var e4sClient: NdlaE4sClient = Elastic4sClientFactory.getClient(props.SearchServer)
  lazy val draftApiClient      = new DraftApiClient
  lazy val searchApiClient     = new SearchApiClient
  lazy val feideApiClient      = new FeideApiClient

  lazy val clock    = new SystemClock
  lazy val authRole = new AuthRole
  lazy val authUser = new AuthUser
}
