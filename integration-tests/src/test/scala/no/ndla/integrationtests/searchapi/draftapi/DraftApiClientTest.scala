/*
 * Part of NDLA integration-tests.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.integrationtests.searchapi.draftapi

import enumeratum.Json4s
import no.ndla.common.DateParser
import no.ndla.common.model.domain.draft.{Draft, DraftStatus}
import no.ndla.common.model.domain.learningpath.EmbedType
import no.ndla.draftapi.DraftApiProperties
import no.ndla.integrationtests.UnitSuite
import no.ndla.network.AuthUser
import no.ndla.scalatestsuite.IntegrationSuite
import no.ndla.search.model.LanguageValue
import no.ndla.searchapi.model.domain.LearningResourceType
import no.ndla.searchapi.model.domain.learningpath._
import no.ndla.{draftapi, searchapi}
import org.eclipse.jetty.server.Server
import org.json4s.Formats
import org.json4s.ext.{EnumNameSerializer, JavaTimeSerializers}
import org.testcontainers.containers.PostgreSQLContainer

class DraftApiClientTest
    extends IntegrationSuite(EnablePostgresContainer = true, EnableElasticsearchContainer = true)
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
      new EnumNameSerializer(LearningResourceType) ++
      JavaTimeSerializers.all

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

  private def setupArticles() = {
    draftApi.componentRegistry.draftRepository.withSession { implicit session =>
      (1 to 10)
        .map(id => {
          draftApi.componentRegistry.draftRepository.insert(
            draftapi.TestData.sampleDomainArticle.copy(
              id = Some(id),
              updated = DateParser.fromUnixTime(0),
              created = DateParser.fromUnixTime(0),
              published = DateParser.fromUnixTime(0)
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
