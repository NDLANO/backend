/*
 * Part of NDLA integration-tests
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.integrationtests.searchapi.articleapi

import no.ndla.articleapi.{ArticleApiProperties, TestData => ArticleTestData}
import no.ndla.common.configuration.Prop
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.search.LanguageValue
import no.ndla.database.HasDatabaseProps
import no.ndla.network.{AuthUser, NdlaClient}
import no.ndla.scalatestsuite.{DatabaseIntegrationSuite, ElasticsearchIntegrationSuite}
import no.ndla.searchapi.integration.ArticleApiClient
import no.ndla.searchapi.model.domain.IndexingBundle
import no.ndla.searchapi.service.ConverterService
import no.ndla.searchapi.service.search.SearchConverterService
import no.ndla.searchapi.{TestData, UnitSuite}
import no.ndla.{articleapi, searchapi}
import org.testcontainers.containers.PostgreSQLContainer

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success, Try}

class ArticleApiClientTest
    extends ElasticsearchIntegrationSuite
    with DatabaseIntegrationSuite
    with UnitSuite
    with searchapi.TestEnvironment
    with HasDatabaseProps {
  override implicit lazy val ndlaClient: NdlaClient                         = new NdlaClient
  override implicit lazy val converterService: ConverterService             = new ConverterService
  override implicit lazy val searchConverterService: SearchConverterService = new SearchConverterService

  val articleApiPort: Int                        = findFreePort
  val pgc: PostgreSQLContainer[?]                = postgresContainer.get
  val esHost: String                             = elasticSearchHost.get
  val articleApiProperties: ArticleApiProperties = new ArticleApiProperties {
    override def ApplicationPort: Int              = articleApiPort
    override val MetaServer: Prop[String]          = propFromTestValue("META_SERVER", pgc.getHost)
    override val MetaResource: Prop[String]        = propFromTestValue("META_RESOURCE", pgc.getDatabaseName)
    override val MetaUserName: Prop[String]        = propFromTestValue("META_USER_NAME", pgc.getUsername)
    override val MetaPassword: Prop[String]        = propFromTestValue("META_PASSWORD", pgc.getPassword)
    override val MetaPort: Prop[Int]               = propFromTestValue("META_PORT", pgc.getMappedPort(5432))
    override val MetaSchema: Prop[String]          = propFromTestValue("META_SCHEMA", "testschema")
    override val BrightcoveAccountId: Prop[String] = propFromTestValue("BRIGHTCOVE_ACCOUNT_ID", "123")
    override val BrightcovePlayerId: Prop[String]  = propFromTestValue("BRIGHTCOVE_PLAYER_ID", "123")
    override def SearchServer: String              = esHost
    override def ArticleSearchIndex: String        = "test-article"
  }

  var articleApi: articleapi.MainClass = null
  val articleApiBaseUrl: String        = s"http://localhost:$articleApiPort"

  override def beforeAll(): Unit = {
    super.beforeAll()
    implicit val ec: ExecutionContextExecutorService =
      ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
    articleApi = new articleapi.MainClass(articleApiProperties)
    Future { articleApi.run(Array.empty) }: Unit

    blockUntilHealthy(s"$articleApiBaseUrl/health/readiness")
  }

  override def afterAll(): Unit = {
    super.afterAll()
  }

  val exampleToken =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjogInh4eHl5eSIsICJpc3MiOiAiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCAic3ViIjogInh4eHl5eUBjbGllbnRzIiwgImF1ZCI6ICJuZGxhX3N5c3RlbSIsICJpYXQiOiAxNTEwMzA1NzczLCAiZXhwIjogMTUxMDM5MjE3MywgInNjb3BlIjogImFydGljbGVzLXRlc3Q6cHVibGlzaCBkcmFmdHMtdGVzdDp3cml0ZSBkcmFmdHMtdGVzdDpzZXRfdG9fcHVibGlzaCBhcnRpY2xlcy10ZXN0OndyaXRlIiwgImd0eSI6ICJjbGllbnQtY3JlZGVudGlhbHMifQ.gsM-U84ykgaxMSbL55w6UYIIQUouPIB6YOmJuj1KhLFnrYctu5vwYBo80zyr1je9kO_6L-rI7SUnrHVao9DFBZJmfFfeojTxIT3CE58hoCdxZQZdPUGePjQzROWRWeDfG96iqhRcepjbVF9pMhKp6FNqEVOxkX00RZg9vFT8iMM"
  val authHeaderMap: Map[String, String] = Map("Authorization" -> s"Bearer $exampleToken")

  class LocalArticleApiTestData {
    implicit lazy val props: ArticleApiProperties = articleApiProperties
    val td                                        = new ArticleTestData

    def setupArticles(): Try[Boolean] =
      (1L to 10)
        .map(id => {
          articleApi.componentRegistry.articleRepository
            .updateArticleFromDraftApi(
              td.sampleDomainArticle.copy(
                id = Some(id),
                updated = NDLADate.fromUnixTime(0),
                created = NDLADate.fromUnixTime(0),
                published = NDLADate.fromUnixTime(0)
              ),
              List(s"1$id")
            )
        })
        .collectFirst { case Failure(ex) => Failure(ex) }
        .getOrElse(Success(true))
  }

  val dataFixer = new LocalArticleApiTestData

  test("that dumping articles returns articles in serializable format") {
    dataFixer.setupArticles()

    AuthUser.setHeader(s"Bearer $exampleToken")
    val articleApiClient = new ArticleApiClient(articleApiBaseUrl)

    val chunks         = articleApiClient.getChunks.toList
    val fetchedArticle = chunks.head.get.head
    val searchable     = searchConverterService
      .asSearchableArticle(
        fetchedArticle,
        IndexingBundle(Some(TestData.emptyGrepBundle), Some(TestData.taxonomyTestBundle), None)
      )

    searchable.isSuccess should be(true)
    searchable.get.title.languageValues should be(Seq(LanguageValue("nb", "title")))
    searchable.get.content.languageValues should be(Seq(LanguageValue("nb", "content")))

  }
}
