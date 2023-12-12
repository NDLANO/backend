/*
 * Part of NDLA integration-tests.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.integrationtests.searchapi.articleapi

import cats.effect.{IO, unsafe}
import enumeratum.Json4s
import no.ndla.articleapi.ArticleApiProperties
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.Availability
import no.ndla.common.model.domain.article.Article
import no.ndla.common.model.domain.draft.DraftStatus
import no.ndla.common.model.domain.learningpath.EmbedType
import no.ndla.network.AuthUser
import no.ndla.scalatestsuite.IntegrationSuite
import no.ndla.search.model.LanguageValue
import no.ndla.searchapi.model.domain.LearningResourceType
import no.ndla.searchapi.model.domain.learningpath._
import no.ndla.searchapi.{TestData, UnitSuite}
import no.ndla.{articleapi, searchapi}
import org.json4s.Formats
import org.json4s.ext.{EnumNameSerializer, JavaTimeSerializers}
import org.testcontainers.containers.PostgreSQLContainer

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class ArticleApiClientTest
    extends IntegrationSuite(EnableElasticsearchContainer = true, EnablePostgresContainer = true)
    with UnitSuite
    with searchapi.TestEnvironment {
  implicit val formats: Formats =
    org.json4s.DefaultFormats +
      Json4s.serializer(DraftStatus) +
      new EnumNameSerializer(LearningPathStatus) +
      new EnumNameSerializer(LearningPathVerificationStatus) +
      new EnumNameSerializer(StepType) +
      new EnumNameSerializer(StepStatus) +
      new EnumNameSerializer(EmbedType) +
      new EnumNameSerializer(LearningResourceType) +
      new EnumNameSerializer(Availability) ++
      JavaTimeSerializers.all +
      NDLADate.Json4sSerializer

  override val ndlaClient             = new NdlaClient
  override val converterService       = new ConverterService
  override val searchConverterService = new SearchConverterService

  val articleApiPort: Int         = findFreePort
  val pgc: PostgreSQLContainer[_] = postgresContainer.get
  val esHost: String              = elasticSearchHost.get
  val articleApiProperties: ArticleApiProperties = new ArticleApiProperties {
    override def ApplicationPort: Int = articleApiPort
    override def MetaServer: String   = pgc.getHost
    override def MetaResource: String = pgc.getDatabaseName
    override def MetaUserName: String = pgc.getUsername
    override def MetaPassword: String = pgc.getPassword
    override def MetaPort: Int        = pgc.getMappedPort(5432)
    override def MetaSchema: String   = "testschema"
    override def SearchServer: String = esHost
  }

  var articleApi: articleapi.MainClass = null
  var cancelFunc: () => Future[Unit]   = null
  val articleApiBaseUrl                = s"http://localhost:$articleApiPort"

  override def beforeAll(): Unit = {
    articleApi = new articleapi.MainClass(articleApiProperties)
    cancelFunc = IO { articleApi.run() }.unsafeRunCancelable()(unsafe.IORuntime.global)

    blockUntil(() => {
      import sttp.client3.quick._
      val req = quickRequest.get(uri"$articleApiBaseUrl/health")
      val res = simpleHttpClient.send(req)
      res.code.code == 200
    })
  }

  override def afterAll(): Unit = {
    super.afterAll()
  }

  val exampleToken =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjogInh4eHl5eSIsICJpc3MiOiAiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCAic3ViIjogInh4eHl5eUBjbGllbnRzIiwgImF1ZCI6ICJuZGxhX3N5c3RlbSIsICJpYXQiOiAxNTEwMzA1NzczLCAiZXhwIjogMTUxMDM5MjE3MywgInNjb3BlIjogImFydGljbGVzLXRlc3Q6cHVibGlzaCBkcmFmdHMtdGVzdDp3cml0ZSBkcmFmdHMtdGVzdDpzZXRfdG9fcHVibGlzaCBhcnRpY2xlcy10ZXN0OndyaXRlIiwgImd0eSI6ICJjbGllbnQtY3JlZGVudGlhbHMifQ.gsM-U84ykgaxMSbL55w6UYIIQUouPIB6YOmJuj1KhLFnrYctu5vwYBo80zyr1je9kO_6L-rI7SUnrHVao9DFBZJmfFfeojTxIT3CE58hoCdxZQZdPUGePjQzROWRWeDfG96iqhRcepjbVF9pMhKp6FNqEVOxkX00RZg9vFT8iMM"
  val authHeaderMap: Map[String, String] = Map("Authorization" -> s"Bearer $exampleToken")

  class LocalArticleApiTestData extends articleapi.Props with articleapi.TestData {
    override val props: ArticleApiProperties = articleApiProperties
    val td                                   = new TestData

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

    val chunks         = articleApiClient.getChunks[Article].toList
    val fetchedArticle = chunks.head.get.head
    val searchable = searchConverterService
      .asSearchableArticle(fetchedArticle, Some(TestData.taxonomyTestBundle), Some(TestData.emptyGrepBundle))

    searchable.isSuccess should be(true)
    searchable.get.title.languageValues should be(Seq(LanguageValue("nb", "title")))
    searchable.get.content.languageValues should be(Seq(LanguageValue("nb", "content")))

  }
}
