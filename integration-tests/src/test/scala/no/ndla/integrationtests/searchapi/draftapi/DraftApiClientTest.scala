/*
 * Part of NDLA integration-tests
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.integrationtests.searchapi.draftapi

import no.ndla.common.configuration.Prop
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.search.LanguageValue
import no.ndla.common.util.TraitUtil
import no.ndla.database.{DBUtility, HasDatabaseProps}
import no.ndla.draftapi.DraftApiProperties
import no.ndla.integrationtests.UnitSuite
import no.ndla.network.NdlaClient
import no.ndla.scalatestsuite.{DatabaseIntegrationSuite, ElasticsearchIntegrationSuite}
import no.ndla.searchapi.integration.DraftApiClient
import no.ndla.searchapi.model.domain.IndexingBundle
import no.ndla.searchapi.service.search.SearchConverterService
import no.ndla.{draftapi, searchapi}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.testcontainers.postgresql.PostgreSQLContainer

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.Success

class DraftApiClientTest
    extends DatabaseIntegrationSuite
    with ElasticsearchIntegrationSuite
    with UnitSuite
    with searchapi.TestEnvironment
    with HasDatabaseProps {
  override implicit lazy val ndlaClient: NdlaClient                         = new NdlaClient
  override implicit lazy val traitUtil: TraitUtil                           = new TraitUtil
  override implicit lazy val searchConverterService: SearchConverterService = new SearchConverterService
  override implicit lazy val DBUtil: DBUtility                              = new DBUtility

  val draftApiPort: Int                      = findFreePort
  val pgc: PostgreSQLContainer               = postgresContainer.get
  val esHost: String                         = elasticSearchHost.get
  val draftApiProperties: DraftApiProperties = new DraftApiProperties {
    override def ApplicationPort: Int                  = draftApiPort
    override val MetaServer: Prop[String]              = propFromTestValue("META_SERVER", pgc.getHost)
    override val MetaResource: Prop[String]            = propFromTestValue("META_RESOURCE", pgc.getDatabaseName)
    override val MetaUserName: Prop[String]            = propFromTestValue("META_USER_NAME", pgc.getUsername)
    override val MetaPassword: Prop[String]            = propFromTestValue("META_PASSWORD", pgc.getPassword)
    override val MetaPort: Prop[Int]                   = propFromTestValue("META_PORT", pgc.getMappedPort(5432))
    override val MetaSchema: Prop[String]              = propFromTestValue("META_SCHEMA", "testschema")
    override val auth0ManagementClientId: Prop[String] =
      propFromTestValue("AUTH0_MANAGEMENT_CLIENT_ID", "auth0_test_id")
    override val auth0ManagementClientSecret: Prop[String] =
      propFromTestValue("AUTH0_MANAGEMENT_CLIENT_SECRET", "auth0_test_secret")
    override val BrightcoveAccountId: Prop[String] = propFromTestValue("BRIGHTCOVE_ACCOUNT_ID", "123")
    override val BrightcovePlayerId: Prop[String]  = propFromTestValue("BRIGHTCOVE_PLAYER_ID", "123")
    override def SearchServer: String              = esHost
    override def DraftSearchIndex: String          = "test-draft"
  }

  var draftApi: draftapi.MainClass = null
  val draftApiBaseUrl: String      = s"http://localhost:$draftApiPort"

  override def beforeAll(): Unit = {
    super.beforeAll()
    when(myndlaApiClient.getStatsFor(any, any)).thenReturn(Success(List.empty))
    implicit val ec: ExecutionContextExecutorService =
      ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
    draftApi = new draftapi.MainClass(draftApiProperties)
    Future {
      draftApi.run(Array.empty)
    }: Unit
    blockUntilHealthy(s"$draftApiBaseUrl/health/readiness")
  }

  override def afterAll(): Unit = {
    super.afterAll()
  }

  private def setupArticles() = {
    draftApi
      .componentRegistry
      .dbUtility
      .writeSession { implicit session =>
        (
          1L to 10
        ).map(id => {
          draftApi
            .componentRegistry
            .draftRepository
            .insert(
              draftapi
                .TestData
                .sampleDomainArticle
                .copy(
                  id = Some(id),
                  created = NDLADate.fromUnixTime(0),
                  updated = NDLADate.fromUnixTime(0),
                  published = Some(NDLADate.fromUnixTime(0)),
                  revised = NDLADate.fromUnixTime(0),
                )
            )
        })
      }
  }

  test("that dumping drafts returns drafts in serializable format") {
    setupArticles()

    val draftApiClient = new DraftApiClient(draftApiBaseUrl)

    val chunks       = draftApiClient.getChunks.toList
    val fetchedDraft = chunks.head.get.head
    val searchable   = searchConverterService.asSearchableDraft(
      fetchedDraft,
      IndexingBundle(Some(searchapi.TestData.emptyGrepBundle), Some(searchapi.TestData.taxonomyTestBundle), None),
    )

    searchable.isSuccess should be(true)
    searchable.get.title.languageValues should be(Seq(LanguageValue("nb", "title")))
    searchable.get.content.languageValues should be(Seq(LanguageValue("nb", "content")))

  }
}
