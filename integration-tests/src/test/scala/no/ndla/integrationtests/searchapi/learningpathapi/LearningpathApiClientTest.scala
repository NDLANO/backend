/*
 * Part of NDLA integration-tests.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.integrationtests.searchapi.learningpathapi

import no.ndla.common.model.NDLADate
import no.ndla.integrationtests.UnitSuite
import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.network.AuthUser
import no.ndla.scalatestsuite.IntegrationSuite
import no.ndla.search.model.LanguageValue
import no.ndla.searchapi.model.domain
import no.ndla.searchapi.model.domain.IndexingBundle
import no.ndla.{learningpathapi, searchapi}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.testcontainers.containers.PostgreSQLContainer

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Success, Try}

class LearningpathApiClientTest
    extends IntegrationSuite(EnableElasticsearchContainer = true, EnablePostgresContainer = true)
    with UnitSuite
    with searchapi.TestEnvironment {
  override val ndlaClient             = new NdlaClient
  override val converterService       = new ConverterService
  override val searchConverterService = new SearchConverterService

  val learningpathApiPort: Int    = findFreePort
  val pgc: PostgreSQLContainer[_] = postgresContainer.get
  val esHost: String              = elasticSearchHost.get
  val learningpathApiProperties: LearningpathApiProperties = new LearningpathApiProperties {
    override def ApplicationPort: Int = learningpathApiPort
    override def MetaServer: String   = pgc.getHost
    override def MetaResource: String = pgc.getDatabaseName
    override def MetaUserName: String = pgc.getUsername
    override def MetaPassword: String = pgc.getPassword
    override def MetaPort: Int        = pgc.getMappedPort(5432)
    override def MetaSchema: String   = "testschema"
    override def SearchServer: String = esHost
  }

  var learningpathApi: learningpathapi.MainClass = null
  val learningpathApiBaseUrl: String             = s"http://localhost:$learningpathApiPort"

  override def beforeAll(): Unit = {
    when(myndlaapiClient.getStatsFor(any, any)).thenReturn(Success(List.empty))
    implicit val ec: ExecutionContextExecutorService =
      ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
    learningpathApi = new learningpathapi.MainClass(learningpathApiProperties)
    Future { learningpathApi.run() }: Unit
    blockUntil(() => {
      import sttp.client3.quick._
      val req = quickRequest.get(uri"$learningpathApiBaseUrl/health")
      val res = Try(simpleHttpClient.send(req))
      res.map(_.code.code) == Success(200)
    })
  }

  override def afterAll(): Unit = {
    super.afterAll()
  }

  private def setupLearningPaths() = {
    (1L to 10)
      .map(id => {
        learningpathApi.componentRegistry.learningPathRepository.insert(
          learningpathapi.TestData.sampleDomainLearningPath
            .copy(id = Some(id), lastUpdated = NDLADate.fromUnixTime(0))
        )
      })
  }

  val exampleToken =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjogInh4eHl5eSIsICJpc3MiOiAiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCAic3ViIjogInh4eHl5eUBjbGllbnRzIiwgImF1ZCI6ICJuZGxhX3N5c3RlbSIsICJpYXQiOiAxNTEwMzA1NzczLCAiZXhwIjogMTUxMDM5MjE3MywgInNjb3BlIjogImFydGljbGVzLXRlc3Q6cHVibGlzaCBkcmFmdHMtdGVzdDp3cml0ZSBkcmFmdHMtdGVzdDpzZXRfdG9fcHVibGlzaCBhcnRpY2xlcy10ZXN0OndyaXRlIiwgImd0eSI6ICJjbGllbnQtY3JlZGVudGlhbHMifQ.gsM-U84ykgaxMSbL55w6UYIIQUouPIB6YOmJuj1KhLFnrYctu5vwYBo80zyr1je9kO_6L-rI7SUnrHVao9DFBZJmfFfeojTxIT3CE58hoCdxZQZdPUGePjQzROWRWeDfG96iqhRcepjbVF9pMhKp6FNqEVOxkX00RZg9vFT8iMM"
  val authHeaderMap: Map[String, String] = Map("Authorization" -> s"Bearer $exampleToken")

  test("that dumping learningpaths returns learningpaths in serializable format") {
    setupLearningPaths()

    AuthUser.setHeader(s"Bearer $exampleToken")
    val learningPathApiClient = new LearningPathApiClient(learningpathApiBaseUrl)

    val chunks              = learningPathApiClient.getChunks[domain.learningpath.LearningPath].toList
    val fetchedLearningPath = chunks.head.get.head

    val searchable =
      searchConverterService.asSearchableLearningPath(
        fetchedLearningPath,
        IndexingBundle(
          None,
          Some(searchapi.TestData.taxonomyTestBundle),
          None
        )
      )

    searchable.isSuccess should be(true)
    searchable.get.title.languageValues should be(Seq(LanguageValue("nb", "tittel")))
    searchable.get.description.languageValues should be(Seq(LanguageValue("nb", "deskripsjon")))
  }

}
