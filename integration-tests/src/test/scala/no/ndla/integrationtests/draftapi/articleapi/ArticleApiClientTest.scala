/*
 * Part of NDLA integration-tests
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.integrationtests.draftapi.articleapi

import no.ndla.{articleapi, draftapi}
import no.ndla.articleapi.ArticleApiProperties
import no.ndla.common.DateParser
import no.ndla.draftapi.model.api.ContentId
import no.ndla.draftapi.model.domain
import no.ndla.draftapi.model.domain.{Article, Availability, Copyright, RevisionMeta, RevisionStatus}
import no.ndla.integrationtests.UnitSuite
import no.ndla.network.AuthUser
import no.ndla.scalatestsuite.IntegrationSuite
import org.eclipse.jetty.server.Server
import org.json4s.Formats
import org.testcontainers.containers.PostgreSQLContainer

import java.time.LocalDateTime
import java.util.UUID
import scala.util.{Failure, Success, Try}

class ArticleApiClientTest
    extends IntegrationSuite(EnableElasticsearchContainer = true, EnablePostgresContainer = true)
    with UnitSuite
    with draftapi.TestEnvironment {
  implicit val formats: Formats = DBArticle.jsonEncoder
  override val ndlaClient       = new NdlaClient

  val articleApiPort: Int         = findFreePort
  val pgc: PostgreSQLContainer[_] = postgresContainer.get
  val esHost: String              = elasticSearchHost.get
  val articleApiProperties: ArticleApiProperties = new ArticleApiProperties {
    override def ApplicationPort: Int = articleApiPort
    override def MetaServer: String   = pgc.getContainerIpAddress
    override def MetaResource: String = pgc.getDatabaseName
    override def MetaUserName: String = pgc.getUsername
    override def MetaPassword: String = pgc.getPassword
    override def MetaPort: Int        = pgc.getMappedPort(5432)
    override def MetaSchema: String   = "testschema"
    override def SearchServer: String = esHost
  }

  val articleApi               = new articleapi.MainClass(articleApiProperties)
  val articleApiServer: Server = articleApi.startServer()

  override def afterAll(): Unit = {
    super.afterAll()
    articleApiServer.stop()
  }

  val idResponse: ContentId     = ContentId(1)
  override val converterService = new ConverterService

  val testCopyright: Copyright = domain.Copyright(
    Some("CC-BY-SA-4.0"),
    Some("Origin"),
    Seq(domain.Author("Writer", "John doe")),
    Seq.empty,
    Seq.empty,
    None,
    None,
    None
  )

  val testArticle: Article = domain.Article(
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
    created = DateParser.fromUnixTime(0),
    updated = DateParser.fromUnixTime(0),
    updatedBy = "updatedBy",
    published = DateParser.fromUnixTime(0),
    articleType = domain.ArticleType.Standard,
    notes = Seq.empty,
    previousVersionsNotes = Seq.empty,
    editorLabels = Seq.empty,
    grepCodes = Seq.empty,
    conceptIds = Seq.empty,
    availability = Availability.everyone,
    relatedContent = Seq.empty,
    revisionMeta = Seq(
      RevisionMeta(
        id = UUID.randomUUID(),
        note = "Revision",
        revisionDate = LocalDateTime.now(),
        status = RevisionStatus.NeedsRevision
      )
    )
  )

  val exampleToken =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjogInh4eHl5eSIsICJpc3MiOiAiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCAic3ViIjogInh4eHl5eUBjbGllbnRzIiwgImF1ZCI6ICJuZGxhX3N5c3RlbSIsICJpYXQiOiAxNTEwMzA1NzczLCAiZXhwIjogMTUxMDM5MjE3MywgInNjb3BlIjogImFydGljbGVzLXRlc3Q6cHVibGlzaCBkcmFmdHMtdGVzdDp3cml0ZSBkcmFmdHMtdGVzdDpzZXRfdG9fcHVibGlzaCBhcnRpY2xlcy10ZXN0OndyaXRlIiwgImd0eSI6ICJjbGllbnQtY3JlZGVudGlhbHMifQ.gsM-U84ykgaxMSbL55w6UYIIQUouPIB6YOmJuj1KhLFnrYctu5vwYBo80zyr1je9kO_6L-rI7SUnrHVao9DFBZJmfFfeojTxIT3CE58hoCdxZQZdPUGePjQzROWRWeDfG96iqhRcepjbVF9pMhKp6FNqEVOxkX00RZg9vFT8iMM"
  val authHeaderMap: Map[String, String] = Map("Authorization" -> s"Bearer $exampleToken")

  class LocalArticleApiTestData extends articleapi.Props with articleapi.TestData {
    override val props: ArticleApiProperties = articleApiProperties
    val td                                   = new TestData

    def setupArticles(): Try[Boolean] =
      (1 to 10)
        .map(id => {
          articleApi.componentRegistry.articleRepository
            .updateArticleFromDraftApi(
              td.sampleDomainArticle.copy(
                id = Some(id),
                updated = DateParser.fromUnixTime(0),
                created = DateParser.fromUnixTime(0),
                published = DateParser.fromUnixTime(0)
              ),
              List(s"1$id")
            )
        })
        .collectFirst { case Failure(ex) => Failure(ex) }
        .getOrElse(Success(true))
  }

  val dataFixer         = new LocalArticleApiTestData
  val articleApiBaseUrl = s"http://localhost:$articleApiPort"

  test("that updating articles should work") {
    dataFixer.setupArticles()

    AuthUser.setHeader(s"Bearer $exampleToken")
    val articleApiClient = new ArticleApiClient(articleApiBaseUrl)
    val response = articleApiClient.updateArticle(
      1,
      testArticle,
      List("1234"),
      useImportValidation = false,
      useSoftValidation = false
    )
    response.isSuccess should be(true)
  }

  test("that deleting an article should return 200") {
    dataFixer.setupArticles()
    val contentId = ContentId(1)
    AuthUser.setHeader(s"Bearer $exampleToken")
    val articleApiClient = new ArticleApiClient(articleApiBaseUrl)
    articleApiClient.deleteArticle(1) should be(Success(contentId))
  }

  test("that unpublishing an article returns 200") {
    dataFixer.setupArticles()
    AuthUser.setHeader(s"Bearer $exampleToken")
    val articleApiCient = new ArticleApiClient(articleApiBaseUrl)
    articleApiCient.unpublishArticle(testArticle).isSuccess should be(true)
  }

  test("that verifying an article returns 200 if valid") {
    val articleApiArticle = converterService.toArticleApiArticle(testArticle)
    AuthUser.setHeader(s"Bearer $exampleToken")
    val articleApiCient = new ArticleApiClient(articleApiBaseUrl)
    val result          = articleApiCient.validateArticle(articleApiArticle, importValidate = false)
    result.isSuccess should be(true)
  }

  test("that verifying an article returns 400 if invalid") {
    val articleApiArticle =
      converterService.toArticleApiArticle(testArticle.copy(title = Seq(domain.ArticleTitle("", "nb"))))
    AuthUser.setHeader(s"Bearer $exampleToken")
    val articleApiCient = new ArticleApiClient(articleApiBaseUrl)
    val result          = articleApiCient.validateArticle(articleApiArticle, importValidate = false)
    result.isSuccess should be(false)
  }
}
