/*
 * Part of NDLA integration-tests
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.integrationtests.draftapi.articleapi

import no.ndla.{articleapi, draftapi}
import no.ndla.articleapi.ArticleApiProperties
import no.ndla.common.model.domain.draft.Draft
import no.ndla.common.model.{NDLADate, domain => common}
import no.ndla.draftapi.model.api.ContentId
import no.ndla.integrationtests.UnitSuite
import no.ndla.network.AuthUser
import no.ndla.scalatestsuite.IntegrationSuite
import org.json4s.Formats
import org.testcontainers.containers.PostgreSQLContainer

import java.util.UUID
import scala.util.{Failure, Success, Try}

class ArticleApiClientTest
    extends IntegrationSuite(EnableElasticsearchContainer = true, EnablePostgresContainer = true)
    with UnitSuite
    with draftapi.TestEnvironment {
  implicit val formats: Formats = Draft.jsonEncoder
  override val ndlaClient       = new NdlaClient

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

  import cats.effect.unsafe.implicits.global
  val articleApi = new articleapi.MainClass(articleApiProperties)
  val cancelFunc = articleApi.run(List.empty).unsafeRunCancelable()

  override def afterAll(): Unit = {
    super.afterAll()
    cancelFunc()
  }

  val idResponse: ContentId     = ContentId(1)
  override val converterService = new ConverterService

  val testCopyright: common.draft.Copyright = common.draft.Copyright(
    Some("CC-BY-SA-4.0"),
    Some("Origin"),
    Seq(common.Author("Writer", "John doe")),
    Seq.empty,
    Seq.empty,
    None,
    None,
    None
  )

  val testArticle: Draft = Draft(
    id = Some(1),
    revision = Some(1),
    status = common.Status(common.draft.DraftStatus.PUBLISHED, Set.empty),
    title = Seq(common.Title("Title", "nb")),
    content = Seq(common.ArticleContent("Content", "nb")),
    copyright = Some(testCopyright),
    tags = Seq(common.Tag(List("Tag1", "Tag2", "Tag3"), "nb")),
    requiredLibraries = Seq(),
    visualElement = Seq(),
    introduction = Seq(),
    metaDescription = Seq(common.Description("Meta Description", "nb")),
    metaImage = Seq(),
    created = NDLADate.fromUnixTime(0),
    updated = NDLADate.fromUnixTime(0),
    updatedBy = "updatedBy",
    published = NDLADate.fromUnixTime(0),
    articleType = common.ArticleType.Standard,
    notes = Seq.empty,
    previousVersionsNotes = Seq.empty,
    editorLabels = Seq.empty,
    grepCodes = Seq.empty,
    conceptIds = Seq.empty,
    availability = common.Availability.everyone,
    relatedContent = Seq.empty,
    revisionMeta = Seq(
      common.draft.RevisionMeta(
        id = UUID.randomUUID(),
        note = "Revision",
        revisionDate = NDLADate.now(),
        status = common.draft.RevisionStatus.NeedsRevision
      )
    ),
    responsible = None,
    slug = None,
    comments = Seq.empty,
    prioritized = false,
    started = false
  )

  val exampleToken =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6Inh4eHl5eSIsImlzcyI6Imh0dHBzOi8vbmRsYS5ldS5hdXRoMC5jb20vIiwic3ViIjoieHh4eXl5QGNsaWVudHMiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTUxMDMwNTc3MywiZXhwIjoxNTEwMzkyMTczLCJwZXJtaXNzaW9ucyI6WyJhcnRpY2xlczpwdWJsaXNoIiwiZHJhZnRzOndyaXRlIiwiZHJhZnRzOnNldF90b19wdWJsaXNoIiwiYXJ0aWNsZXM6d3JpdGUiXSwiZ3R5IjoiY2xpZW50LWNyZWRlbnRpYWxzIn0.v6q6y6owx9VXri1h4FJJHDAnMllmNYFAAT2b9CJLm88"
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
    articleApiClient.deleteArticle(1).get should be(contentId)
  }

  test("that unpublishing an article returns 200") {
    dataFixer.setupArticles()
    AuthUser.setHeader(s"Bearer $exampleToken")
    val articleApiCient = new ArticleApiClient(articleApiBaseUrl)
    articleApiCient.unpublishArticle(testArticle).get
  }

  test("that verifying an article returns 200 if valid") {
    AuthUser.setHeader(s"Bearer $exampleToken")
    val articleApiCient = new ArticleApiClient(articleApiBaseUrl)
    val result = converterService
      .toArticleApiArticle(testArticle)
      .flatMap(article => articleApiCient.validateArticle(article, importValidate = false))
    println(result)
    result.isSuccess should be(true)
  }

  test("that verifying an article returns 400 if invalid") {
    AuthUser.setHeader(s"Bearer $exampleToken")
    val articleApiCient = new ArticleApiClient(articleApiBaseUrl)
    val result = converterService
      .toArticleApiArticle(testArticle.copy(title = Seq(common.Title("", "nb"))))
      .flatMap(article => articleApiCient.validateArticle(article, importValidate = false))
    result.isSuccess should be(false)
  }
}
