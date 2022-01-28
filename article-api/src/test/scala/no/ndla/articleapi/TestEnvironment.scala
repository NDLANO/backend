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
import no.ndla.articleapi.caching.Memoize
import no.ndla.articleapi.controller._
import no.ndla.articleapi.integration._
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service._
import no.ndla.articleapi.service.search._
import no.ndla.articleapi.validation.ContentValidator
import no.ndla.articleapi.integration.SearchApiClient
import no.ndla.articleapi.model.domain.DBArticleSupport
import no.ndla.network.NdlaClient
import no.ndla.search.{BaseIndexService, Elastic4sClient, NdlaE4sClient}
import org.mockito.scalatest.MockitoSugar

trait TestEnvironment
    extends Elastic4sClient
    with ArticleApiPropertiesT
    with DBArticleSupport
    with ArticleSearchService
    with ArticleIndexService
    with IndexService
    with BaseIndexService
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
    with Memoize
    with WriteService
    with ContentValidator
    with Clock
    with ArticleApiInfo
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

  val ArticleApiProperties: PropBase = new PropBase {
    override val Environment = "local"
    override val SearchServer: String = "some-server"
    override val RunWithSignedSearchRequests = false

    override val AudioHost: String = "localhost:30014"
    override val ImageHost: String = "localhost:30001"
    override val DraftHost: String = "localhost:30022"

    override val BrightcoveAccountId: String = "some-account-id"
    override val BrightcovePlayerId: String = "some-player-id"

    override lazy val Domain = ""

    override val ArticleSearchIndex: String = "article-integration-test-index"
    override val IsKubernetes: Boolean = false
    override val ApplicationName: String = "article-api"
    override val Auth0LoginEndpoint: String = ???
    override val RoleWithWriteAccess: String = ???
    override val DraftRoleWithWriteAccess: String = ???
    override val ApplicationPort: Int = ???
    override val DefaultLanguage: String = ???
    override val ContactName: String = ???
    override val ContactUrl: String = ???
    override val ContactEmail: String = ???
    override val TermsUrl: String = ???
    override val MetaUserName: String = ???
    override val MetaPassword: String = ???
    override val MetaResource: String = ???
    override val MetaServer: String = ???
    override val MetaPort: Int = ???
    override val MetaSchema: String = ???
    override val MetaMaxConnections: Int = ???
    override val oldCreatorTypes: List[String] = ???
    override val creatorTypes: List[String] = ???
    override val oldProcessorTypes: List[String] = ???
    override val processorTypes: List[String] = ???
    override val oldRightsholderTypes: List[String] = ???
    override val rightsholderTypes: List[String] = ???
    override val allowedAuthors: List[String] = ???
    override val maxConvertionRounds: Int = ???
    override val ArticleSearchDocument: String = ???
    override val DefaultPageSize: Int = ???
    override val MaxPageSize: Int = ???
    override val IndexBulkSize: Int = ???
    override val ElasticSearchIndexMaxResultWindow: Int = ???
    override val ElasticSearchScrollKeepAlive: String = ???
    override val InitialScrollContextKeywords: List[String] = ???
    override val CorrelationIdKey: String = ???
    override val CorrelationIdHeader: String = ???
    override val SearchHost: String = ???
    override val ApiClientsCacheAgeInMs: Long = ???
    override val MinimumAllowedTags: Int = ???
    override val externalApiUrls: Map[String, String] = ???
    override val H5PAddress: String = ???
    override val BrightcoveVideoScriptUrl: String = ???
    override val H5PResizerScriptUrl: String = ???
    override val NRKVideoScriptUrl: Seq[String] = ???
  }

}
