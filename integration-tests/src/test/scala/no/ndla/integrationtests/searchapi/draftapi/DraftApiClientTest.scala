/*
 * Part of NDLA integration-tests.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.integrationtests.searchapi.draftapi

import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.draft.Draft
import no.ndla.draftapi.DraftApiProperties
import no.ndla.integrationtests.UnitSuite
import no.ndla.network.AuthUser
import no.ndla.scalatestsuite.IntegrationSuite
import no.ndla.search.model.LanguageValue
import no.ndla.{draftapi, searchapi}
import org.testcontainers.containers.PostgreSQLContainer

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Success, Try}

class DraftApiClientTest
    extends IntegrationSuite(EnablePostgresContainer = true, EnableElasticsearchContainer = true)
    with UnitSuite
    with searchapi.TestEnvironment {
  override val ndlaClient             = new NdlaClient
  override val searchConverterService = new SearchConverterService

  val draftApiPort: Int           = findFreePort
  val pgc: PostgreSQLContainer[_] = postgresContainer.get
  val esHost: String              = elasticSearchHost.get
  val draftApiProperties: DraftApiProperties = new DraftApiProperties {
    override def ApplicationPort: Int = draftApiPort
    override def MetaServer: String   = pgc.getHost
    override def MetaResource: String = pgc.getDatabaseName
    override def MetaUserName: String = pgc.getUsername
    override def MetaPassword: String = pgc.getPassword
    override def MetaPort: Int        = pgc.getMappedPort(5432)
    override def MetaSchema: String   = "testschema"
    override def SearchServer: String = esHost
  }

  var draftApi: draftapi.MainClass = null
  val draftApiBaseUrl: String      = s"http://localhost:$draftApiPort"

  override def beforeAll(): Unit = {
    implicit val ec: ExecutionContextExecutorService =
      ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
    draftApi = new draftapi.MainClass(draftApiProperties)
    Future { draftApi.run() }: Unit
    blockUntil(() => {
      import sttp.client3.quick._
      val req = quickRequest.get(uri"$draftApiBaseUrl/health")
      val res = Try(simpleHttpClient.send(req))
      res.map(_.code.code) == Success(200)
    })
  }

  override def afterAll(): Unit = {
    super.afterAll()
  }

  private def setupArticles() = {
    draftApi.componentRegistry.draftRepository.withSession { implicit session =>
      (1L to 10)
        .map(id => {
          draftApi.componentRegistry.draftRepository.insert(
            draftapi.TestData.sampleDomainArticle.copy(
              id = Some(id),
              updated = NDLADate.fromUnixTime(0),
              created = NDLADate.fromUnixTime(0),
              published = NDLADate.fromUnixTime(0)
            )
          )
        })
    }
  }

  val exampleToken =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjogInh4eHl5eSIsICJpc3MiOiAiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCAic3ViIjogInh4eHl5eUBjbGllbnRzIiwgImF1ZCI6ICJuZGxhX3N5c3RlbSIsICJpYXQiOiAxNTEwMzA1NzczLCAiZXhwIjogMTUxMDM5MjE3MywgInNjb3BlIjogImFydGljbGVzLXRlc3Q6cHVibGlzaCBkcmFmdHMtdGVzdDp3cml0ZSBkcmFmdHMtdGVzdDpzZXRfdG9fcHVibGlzaCBhcnRpY2xlcy10ZXN0OndyaXRlIiwgImd0eSI6ICJjbGllbnQtY3JlZGVudGlhbHMifQ.gsM-U84ykgaxMSbL55w6UYIIQUouPIB6YOmJuj1KhLFnrYctu5vwYBo80zyr1je9kO_6L-rI7SUnrHVao9DFBZJmfFfeojTxIT3CE58hoCdxZQZdPUGePjQzROWRWeDfG96iqhRcepjbVF9pMhKp6FNqEVOxkX00RZg9vFT8iMM"
  val authHeaderMap: Map[String, String] = Map("Authorization" -> s"Bearer $exampleToken")

  test("that dumping drafts returns drafts in serializable format") {
    setupArticles()

    AuthUser.setHeader(s"Bearer $exampleToken")
    val draftApiClient = new DraftApiClient(draftApiBaseUrl)

    val chunks       = draftApiClient.getChunks[Draft].toList
    val fetchedDraft = chunks.head.get.head
    val searchable = searchConverterService
      .asSearchableDraft(
        fetchedDraft,
        Some(searchapi.TestData.taxonomyTestBundle),
        Some(searchapi.TestData.emptyGrepBundle)
      )

    searchable.isSuccess should be(true)
    searchable.get.title.languageValues should be(Seq(LanguageValue("nb", "title")))
    searchable.get.content.languageValues should be(Seq(LanguageValue("nb", "content")))

  }
}
