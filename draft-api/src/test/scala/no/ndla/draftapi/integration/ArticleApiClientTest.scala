/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.integration

import com.zaxxer.hikari.HikariDataSource

import java.util.Date
import no.ndla.draftapi.model.api.ContentId
import no.ndla.draftapi.model.domain
import no.ndla.draftapi.model.domain.Availability
import no.ndla.draftapi.{TestEnvironment, UnitSuite}
import no.ndla.network.AuthUser
import no.ndla.scalatestsuite.IntegrationSuite
import org.eclipse.jetty.server.Server
import org.joda.time.DateTime
import org.json4s.native.Serialization.write
import org.json4s.Formats
import no.ndla.articleapi

import scala.util.{Failure, Success}
import scala.xml.Properties.setProp

class ArticleApiClientTest extends IntegrationSuite(EnablePostgresContainer = true) with TestEnvironment {

  setProp("NDLA_ENVIRONMENT", "local")
  setProp("ENABLE_JOUBEL_H5P_OEMBED", "true")

  setProp("SEARCH_SERVER", "some-server")
  setProp("SEARCH_REGION", "some-region")
  setProp("RUN_WITH_SIGNED_SEARCH_REQUESTS", "false")
  setProp("SEARCH_INDEX_NAME", "draft-integration-test-index")
  setProp("AGREEMENT_SEARCH_INDEX_NAME", "agreement-integration-test-index")

  setProp("AUDIO_API_URL", "localhost:30014")
  setProp("IMAGE_API_URL", "localhost:30001")

  setProp("NDLA_BRIGHTCOVE_ACCOUNT_ID", "some-account-id")
  setProp("NDLA_BRIGHTCOVE_PLAYER_ID", "some-player-id")
  setProp("BRIGHTCOVE_API_CLIENT_ID", "some-client-id")
  setProp("BRIGHTCOVE_API_CLIENT_SECRET", "some-secret")

  override val dataSource: HikariDataSource = testDataSource.get

  implicit val formats: Formats = domain.Article.jsonEncoder
  override val ndlaClient = new NdlaClient

  // Pact CDC imports
  import com.itv.scalapact.ScalaPactForger._
  import com.itv.scalapact.circe13._
  import com.itv.scalapact.http4s21._

  val idResponse = ContentId(1)
  override val converterService = new ConverterService

  val testCopyright = domain.Copyright(
    Some("CC-BY-SA-4.0"),
    Some("Origin"),
    Seq(domain.Author("Writer", "John doe")),
    Seq.empty,
    Seq.empty,
    None,
    None,
    None
  )

  val testArticle = domain.Article(
    id = Some(1),
    revision = Some(1),
    status = domain.Status(domain.ArticleStatus.PUBLISHED, Set.empty),
    title = Seq(domain.ArticleTitle("Title", "nb")),
    content = Seq(domain.ArticleContent("Content", "nb")),
    copyright = Some(testCopyright),
    tags = Seq(domain.ArticleTag(List("Tag1", "Tag2", "Tag3"), "nb")),
    requiredLibraries = Seq(),
    visualElement = Seq(),
    introduction = Seq(),
    metaDescription = Seq(domain.ArticleMetaDescription("Meta Description", "nb")),
    metaImage = Seq(),
    created = new Date(0),
    updated = new Date(0),
    updatedBy = "updatedBy",
    published = new Date(0),
    articleType = domain.ArticleType.Standard,
    notes = Seq.empty,
    previousVersionsNotes = Seq.empty,
    editorLabels = Seq.empty,
    grepCodes = Seq.empty,
    conceptIds = Seq.empty,
    availability = Availability.everyone,
    relatedContent = Seq.empty
  )

  val exampleToken =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjogInh4eHl5eSIsICJpc3MiOiAiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCAic3ViIjogInh4eHl5eUBjbGllbnRzIiwgImF1ZCI6ICJuZGxhX3N5c3RlbSIsICJpYXQiOiAxNTEwMzA1NzczLCAiZXhwIjogMTUxMDM5MjE3MywgInNjb3BlIjogImFydGljbGVzLXRlc3Q6cHVibGlzaCBkcmFmdHMtdGVzdDp3cml0ZSBkcmFmdHMtdGVzdDpzZXRfdG9fcHVibGlzaCBhcnRpY2xlcy10ZXN0OndyaXRlIiwgImd0eSI6ICJjbGllbnQtY3JlZGVudGlhbHMifQ.gsM-U84ykgaxMSbL55w6UYIIQUouPIB6YOmJuj1KhLFnrYctu5vwYBo80zyr1je9kO_6L-rI7SUnrHVao9DFBZJmfFfeojTxIT3CE58hoCdxZQZdPUGePjQzROWRWeDfG96iqhRcepjbVF9pMhKp6FNqEVOxkX00RZg9vFT8iMM"
  val authHeaderMap = Map("Authorization" -> s"Bearer $exampleToken")

  val articleApiPort: Int = findFreePort

  val articleApiProps: articleapi.ArticleApiProperties = new articleapi.ArticleApiProperties {
    override def ApplicationPort: Int = articleApiPort

    override def MetaUserName: String = {
      postgresContainer.get.getUsername
    }
    override def MetaPassword: String = postgresContainer.get.getPassword
    override def MetaServer: String = postgresContainer.get.getContainerIpAddress
    override def MetaPort: Int = postgresContainer.get.getMappedPort(5432)
    override def MetaResource: String = postgresContainer.get.getDatabaseName
    override def MetaSchema: String = "testschema"
  }

  val articleApi = new articleapi.MainClass(articleApiProps)
  val articleApiServer: Server = articleApi.startServer()

  private def setupArticles() =
    (1 to 10)
      .map(id => {
        articleApi.componentRegistry.articleRepository
          .updateArticleFromDraftApi(
            articleapi.TestData.sampleDomainArticle.copy(
              id = Some(id),
              updated = new DateTime(0).toDate,
              created = new DateTime(0).toDate,
              published = new DateTime(0).toDate
            ),
            List(s"1$id")
          )
      })
      .collectFirst { case Failure(ex) => Failure(ex) }
      .getOrElse(Success(true))

  test("that updating articles should work") {
    setupArticles()
    AuthUser.setHeader(s"Bearer $exampleToken")
    val articleApiClient = new ArticleApiClient(s"http://localhost:$articleApiPort")
    val res = articleApiClient.updateArticle(1, testArticle, List("1234"), false, false)
    res.get
  }

  test("that deleting an article should return 200") {
    val contentId = ContentId(1)

    forgePact
      .between("draft-api")
      .and("article-api")
      .addInteraction(
        interaction
          .description("Deleting an article should return 200")
          .given("articles")
          .uponReceiving(method = DELETE,
                         path = "/intern/article/1/",
                         query = None,
                         headers = authHeaderMap,
                         body = None,
                         matchingRules = None)
          .willRespondWith(200, write(contentId))
      )
      .runConsumerTest { mockConfig =>
        AuthUser.setHeader(s"Bearer $exampleToken")
        val articleApiClient = new ArticleApiClient(mockConfig.baseUrl)
        articleApiClient.deleteArticle(1) should be(Success(contentId))
      }
  }

  test("that unpublishing an article returns 200") {
    forgePact
      .between("draft-api")
      .and("article-api")
      .addInteraction(
        interaction
          .description("Unpublishing an article should return 200")
          .given("articles")
          .uponReceiving(method = POST,
                         path = "/intern/article/1/unpublish/",
                         query = None,
                         headers = authHeaderMap,
                         body = None,
                         matchingRules = None)
          .willRespondWith(200, write(ContentId(1)))
      )
      .runConsumerTest { mockConfig =>
        AuthUser.setHeader(s"Bearer $exampleToken")
        val articleApiCient = new ArticleApiClient(mockConfig.baseUrl)
        articleApiCient.unpublishArticle(testArticle).isSuccess should be(true)
      }
  }

  test("that verifying an article returns 200 if valid") {
    val articleApiArticle = converterService.toArticleApiArticle(testArticle)
    forgePact
      .between("draft-api")
      .and("article-api")
      .addInteraction(
        interaction
          .description("Validating article returns 200")
          .given("empty")
          .uponReceiving(method = POST,
                         path = "/intern/validate/article",
                         query = None,
                         headers = authHeaderMap,
                         body = write(articleApiArticle),
                         matchingRules = None)
          .willRespondWith(200, write(articleApiArticle))
      )
      .runConsumerTest { mockConfig =>
        AuthUser.setHeader(s"Bearer $exampleToken")
        val articleApiCient = new ArticleApiClient(mockConfig.baseUrl)
        val result = articleApiCient.validateArticle(articleApiArticle, importValidate = false)
        result.isSuccess should be(true)
      }
  }
}
