/*
 * Part of NDLA integration-tests
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.integrationtests.draftapi.articleapi

import no.ndla.articleapi.ArticleApiProperties
import no.ndla.common.model.domain.Priority
import no.ndla.common.model.domain.draft.Draft
import no.ndla.common.model.{NDLADate, domain as common}
import no.ndla.draftapi.model.api.ContentIdDTO
import no.ndla.integrationtests.UnitSuite
import no.ndla.network.AuthUser
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.scalatestsuite.IntegrationSuite
import no.ndla.validation.HtmlTagRules
import no.ndla.{articleapi, draftapi}
import org.testcontainers.containers.PostgreSQLContainer

import java.util.UUID
import java.util.concurrent.Executors
import scala.annotation.unused
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success, Try}

class ArticleApiClientTest
    extends IntegrationSuite(EnableElasticsearchContainer = true, EnablePostgresContainer = true)
    with UnitSuite
    with draftapi.TestEnvironment {
  override val ndlaClient = new NdlaClient

  // NOTE: There is some weirdness with loading the resources in validation library if this isn't called.
  //       For some reason this fixes that.
  //       No idea why.
  @unused
  val WeNeedThisToMakeTheTestsWorkNoIdeaWhyReadTheComment: Set[String] = HtmlTagRules.PermittedHTML.tags

  val articleApiPort: Int         = findFreePort
  val pgc: PostgreSQLContainer[?] = postgresContainer.get
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
  val articleApiBaseUrl: String        = s"http://localhost:$articleApiPort"

  override def beforeAll(): Unit = {
    implicit val ec: ExecutionContextExecutorService =
      ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
    articleApi = new articleapi.MainClass(articleApiProperties)
    Future { articleApi.run() }: Unit
    blockUntil(() => {
      import sttp.client3.quick.*
      val req = quickRequest.get(uri"$articleApiBaseUrl/health/readiness")
      val res = Try(simpleHttpClient.send(req))
      println(res)
      res.map(_.code.code) == Success(200)
    })
  }

  override def afterAll(): Unit = {
    super.afterAll()
  }

  val idResponse: ContentIdDTO  = ContentIdDTO(1)
  override val converterService = new ConverterService

  val testCopyright: common.draft.DraftCopyright = common.draft.DraftCopyright(
    Some("CC-BY-SA-4.0"),
    Some("Origin"),
    Seq(common.Author("Writer", "John doe")),
    Seq.empty,
    Seq.empty,
    None,
    None,
    false
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
    priority = Priority.Unspecified,
    started = false,
    qualityEvaluation = None
  )

  val exampleToken =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6Inh4eHl5eSIsImlzcyI6Imh0dHBzOi8vbmRsYS5ldS5hdXRoMC5jb20vIiwic3ViIjoieHh4eXl5QGNsaWVudHMiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTUxMDMwNTc3MywiZXhwIjoxNTEwMzkyMTczLCJwZXJtaXNzaW9ucyI6WyJhcnRpY2xlczpwdWJsaXNoIiwiZHJhZnRzOndyaXRlIiwiZHJhZnRzOnNldF90b19wdWJsaXNoIiwiYXJ0aWNsZXM6d3JpdGUiXSwiZ3R5IjoiY2xpZW50LWNyZWRlbnRpYWxzIn0.v6q6y6owx9VXri1h4FJJHDAnMllmNYFAAT2b9CJLm88"
  val authHeaderMap: Map[String, String] = Map("Authorization" -> s"Bearer $exampleToken")
  val authUser: TokenUser                = TokenUser.SystemUser.copy(originalToken = Some(exampleToken))

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

  test("that updating articles should work") {
    dataFixer.setupArticles()

    AuthUser.setHeader(s"Bearer $exampleToken")
    val articleApiClient = new ArticleApiClient(articleApiBaseUrl)
    val response = articleApiClient.updateArticle(
      1,
      testArticle,
      List("1234"),
      useImportValidation = false,
      useSoftValidation = false,
      authUser
    )
    response.isSuccess should be(true)
  }

  test("that deleting an article should return 200") {
    dataFixer.setupArticles()
    val contentId = ContentIdDTO(1)
    AuthUser.setHeader(s"Bearer $exampleToken")
    val articleApiClient = new ArticleApiClient(articleApiBaseUrl)
    articleApiClient.deleteArticle(1, authUser).get should be(contentId)
  }

  test("that unpublishing an article returns 200") {
    dataFixer.setupArticles()
    AuthUser.setHeader(s"Bearer $exampleToken")
    val articleApiCient = new ArticleApiClient(articleApiBaseUrl)
    articleApiCient.unpublishArticle(testArticle, authUser).get
  }

  test("that verifying an article returns 200 if valid") {
    AuthUser.setHeader(s"Bearer $exampleToken")
    val articleApiCient = new ArticleApiClient(articleApiBaseUrl)
    val result = converterService
      .toArticleApiArticle(testArticle)
      .flatMap(article => articleApiCient.validateArticle(article, importValidate = false, None))
    result.isSuccess should be(true)
  }

  test("that verifying an article returns 400 if invalid") {
    AuthUser.setHeader(s"Bearer $exampleToken")
    val articleApiCient = new ArticleApiClient(articleApiBaseUrl)
    val result = converterService
      .toArticleApiArticle(testArticle.copy(title = Seq(common.Title("", "nb"))))
      .flatMap(article => articleApiCient.validateArticle(article, importValidate = false, None))
    result.isSuccess should be(false)
  }

  test("that updating an article returns 400 if missing required field") {
    AuthUser.setHeader(s"Bearer $exampleToken")
    val articleApiCient = new ArticleApiClient(articleApiBaseUrl)
    val invalidArticle  = testArticle.copy(metaDescription = Seq.empty)
    val result = articleApiCient.updateArticle(
      id = 10,
      draft = invalidArticle,
      externalIds = List.empty,
      useImportValidation = false,
      useSoftValidation = false,
      user = authUser
    )

    result.isSuccess should be(false)
  }
}
