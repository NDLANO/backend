/*
 * Part of NDLA integration-tests.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.integrationtests.searchapi.draftapi

import no.ndla.draftapi.DraftApiProperties
import no.ndla.integrationtests.UnitSuite
import no.ndla.network.AuthUser
import no.ndla.scalatestsuite.IntegrationSuite
import no.ndla.search.model.LanguageValue
import no.ndla.searchapi.model.domain
import no.ndla.searchapi.model.domain.article.LearningResourceType
import no.ndla.searchapi.model.domain.draft.ArticleStatus
import no.ndla.searchapi.model.domain.learningpath._
import no.ndla.{draftapi, searchapi}
import org.eclipse.jetty.server.Server
import org.joda.time.DateTime
import org.json4s.Formats
import org.json4s.ext.EnumNameSerializer
import org.testcontainers.containers.PostgreSQLContainer

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

class DraftApiClientTest
    extends IntegrationSuite(EnablePostgresContainer = true, EnableElasticsearchContainer = true)
    with UnitSuite
    with searchapi.TestEnvironment {
  implicit val formats: Formats =
    org.json4s.DefaultFormats +
      new EnumNameSerializer(ArticleStatus) +
      new EnumNameSerializer(LearningPathStatus) +
      new EnumNameSerializer(LearningPathVerificationStatus) +
      new EnumNameSerializer(StepType) +
      new EnumNameSerializer(StepStatus) +
      new EnumNameSerializer(EmbedType) +
      new EnumNameSerializer(LearningResourceType) ++
      org.json4s.ext.JodaTimeSerializers.all

  override val ndlaClient             = new NdlaClient
  override val searchConverterService = new SearchConverterService

  val draftApiPort: Int           = findFreePort
  val pgc: PostgreSQLContainer[_] = postgresContainer.get
  val esHost: String              = elasticSearchHost.get
  val draftApiProperties: DraftApiProperties = new DraftApiProperties {
    override def ApplicationPort: Int = draftApiPort
    override def MetaServer: String   = pgc.getContainerIpAddress
    override def MetaResource: String = pgc.getDatabaseName
    override def MetaUserName: String = pgc.getUsername
    override def MetaPassword: String = pgc.getPassword
    override def MetaPort: Int        = pgc.getMappedPort(5432)
    override def MetaSchema: String   = "testschema"
    override def SearchServer: String = esHost
  }

  val draftApi               = new draftapi.MainClass(draftApiProperties)
  val draftApiServer: Server = draftApi.startServer()
  val draftApiBaseUrl        = s"http://localhost:$draftApiPort"

  override def afterAll(): Unit = {
    super.afterAll()
    draftApiServer.stop()
  }

  private def setupArticles() =
    (1 to 10)
      .map(id => {
        draftApi.componentRegistry.draftRepository.insert(
          draftapi.TestData.sampleDomainArticle.copy(
            id = Some(id),
            updated = new DateTime(0).toDate,
            created = new DateTime(0).toDate,
            published = new DateTime(0).toDate
          )
        )
      })

  val exampleToken =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjogInh4eHl5eSIsICJpc3MiOiAiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCAic3ViIjogInh4eHl5eUBjbGllbnRzIiwgImF1ZCI6ICJuZGxhX3N5c3RlbSIsICJpYXQiOiAxNTEwMzA1NzczLCAiZXhwIjogMTUxMDM5MjE3MywgInNjb3BlIjogImFydGljbGVzLXRlc3Q6cHVibGlzaCBkcmFmdHMtdGVzdDp3cml0ZSBkcmFmdHMtdGVzdDpzZXRfdG9fcHVibGlzaCBhcnRpY2xlcy10ZXN0OndyaXRlIiwgImd0eSI6ICJjbGllbnQtY3JlZGVudGlhbHMifQ.gsM-U84ykgaxMSbL55w6UYIIQUouPIB6YOmJuj1KhLFnrYctu5vwYBo80zyr1je9kO_6L-rI7SUnrHVao9DFBZJmfFfeojTxIT3CE58hoCdxZQZdPUGePjQzROWRWeDfG96iqhRcepjbVF9pMhKp6FNqEVOxkX00RZg9vFT8iMM"
  val authHeaderMap = Map("Authorization" -> s"Bearer $exampleToken")

  test("that dumping drafts returns drafts in serializable format") {
    setupArticles()

    val today = new DateTime(0)
    withFrozenTime(today) {

      AuthUser.setHeader(s"Bearer $exampleToken")
      val draftApiClient = new DraftApiClient(draftApiBaseUrl)

      implicit val ec  = ExecutionContext.global
      val chunks       = draftApiClient.getChunks[domain.draft.Draft].toList
      val fetchedDraft = Await.result(chunks.head, Duration.Inf).get.head
      val searchable = searchConverterService
        .asSearchableDraft(
          fetchedDraft,
          searchapi.TestData.taxonomyTestBundle,
          Some(searchapi.TestData.emptyGrepBundle)
        )

      searchable.isSuccess should be(true)
      searchable.get.title.languageValues should be(Seq(LanguageValue("nb", "title")))
      searchable.get.content.languageValues should be(Seq(LanguageValue("nb", "content")))
    }
  }
}
