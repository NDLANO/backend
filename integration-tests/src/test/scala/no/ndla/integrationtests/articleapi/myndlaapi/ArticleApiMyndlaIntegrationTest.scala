/*
 * Part of NDLA integration-tests
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.integrationtests.articleapi.myndlaapi

import no.ndla.articleapi.{ArticleApiProperties, TestData as ArticleTestData}
import no.ndla.common.configuration.Prop
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.Availability
import no.ndla.integrationtests.UnitSuite
import no.ndla.myndlaapi.{ComponentRegistry, MyNdlaApiProperties}
import no.ndla.network.NdlaClient
import no.ndla.network.clients.{FeideApiClient, FeideExtendedUserInfo}
import no.ndla.scalatestsuite.DatabaseIntegrationSuite
import no.ndla.{articleapi, myndlaapi}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{when, withSettings}
import org.mockito.quality.Strictness
import org.scalatestplus.mockito.MockitoSugar
import org.testcontainers.postgresql.PostgreSQLContainer
import sttp.client3.quick.*

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success, Try}

class ArticleApiMyndlaIntegrationTest extends DatabaseIntegrationSuite with UnitSuite with MockitoSugar {
  override def setPropEnv(key: String, value: String): String = {
    sys.props.put(key, value)
    value
  }

  implicit lazy val ndlaClient: NdlaClient = new NdlaClient

  val myndlaApiPort: Int    = findFreePort
  val articleApiPort: Int   = findFreePort
  val articleSchema: String = "article_testschema"
  val myndlaSchema: String  = "myndla_testschema"

  override lazy val schemaName: String = articleSchema
  val pgc: PostgreSQLContainer         = postgresContainer.get

  val myndlaApiProperties: MyNdlaApiProperties = new MyNdlaApiProperties {
    override def ApplicationPort: Int = myndlaApiPort

    override val MetaServer: Prop[String]           = propFromTestValue("META_SERVER", pgc.getHost)
    override val MetaResource: Prop[String]         = propFromTestValue("META_RESOURCE", pgc.getDatabaseName)
    override val MetaUserName: Prop[String]         = propFromTestValue("META_USER_NAME", pgc.getUsername)
    override val MetaPassword: Prop[String]         = propFromTestValue("META_PASSWORD", pgc.getPassword)
    override val MetaPort: Prop[Int]                = propFromTestValue("META_PORT", pgc.getMappedPort(5432))
    override val MetaSchema: Prop[String]           = propFromTestValue("META_SCHEMA", myndlaSchema)
    override def MetaMigrationTable: Option[String] = Some("myndla_schema_version")
    override def disableWarmup: Boolean             = true
  }

  val articleApiProperties: ArticleApiProperties = new ArticleApiProperties {
    override def ApplicationPort: Int              = articleApiPort
    override val MetaServer: Prop[String]          = propFromTestValue("META_SERVER", pgc.getHost)
    override val MetaResource: Prop[String]        = propFromTestValue("META_RESOURCE", pgc.getDatabaseName)
    override val MetaUserName: Prop[String]        = propFromTestValue("META_USER_NAME", pgc.getUsername)
    override val MetaPassword: Prop[String]        = propFromTestValue("META_PASSWORD", pgc.getPassword)
    override val MetaPort: Prop[Int]               = propFromTestValue("META_PORT", pgc.getMappedPort(5432))
    override val MetaSchema: Prop[String]          = propFromTestValue("META_SCHEMA", articleSchema)
    override val BrightcoveAccountId: Prop[String] = propFromTestValue("BRIGHTCOVE_ACCOUNT_ID", "123")
    override val BrightcovePlayerId: Prop[String]  = propFromTestValue("BRIGHTCOVE_PLAYER_ID", "123")
    override def MyNDLAApiHost: String             = s"localhost:$myndlaApiPort"
    override def disableWarmup: Boolean            = true
  }
  override lazy val props: ArticleApiProperties = new ArticleApiProperties
  given ArticleApiProperties                    = articleApiProperties

  var articleApi: articleapi.MainClass = null
  var myndlaApi: myndlaapi.MainClass   = null
  val articleApiBaseUrl: String        = s"http://localhost:${articleApiProperties.ApplicationPort}"
  val myndlaApiBaseUrl: String         = s"http://localhost:${myndlaApiProperties.ApplicationPort}"

  override def beforeAll(): Unit = {
    super.beforeAll()
    println(s"Article API port: $articleApiPort, MyNDLA API port: $myndlaApiPort")
    try articleApiProperties.throwIfFailedProps()
    catch {
      case ex: Throwable =>
        println(s"articleApiProperties.throwIfFailedProps failed: ${ex.getMessage}");
        ex.printStackTrace()
    }
    try myndlaApiProperties.throwIfFailedProps()
    catch {
      case ex: Throwable =>
        println(s"myndlaApiProperties.throwIfFailedProps failed: ${ex.getMessage}");
        ex.printStackTrace()
    }
    implicit val ec: ExecutionContextExecutorService =
      ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(2))

    myndlaApi = new myndlaapi.MainClass(myndlaApiProperties) {
      override val componentRegistry: ComponentRegistry = new ComponentRegistry(myndlaApiProperties) {
        override implicit lazy val feideApiClient: FeideApiClient =
          mock[FeideApiClient](withSettings().strictness(Strictness.LENIENT))
      }
    }

    Future {
      val result = myndlaApi.run(Array.empty)
      result
        .failed
        .foreach(ex => {
          println(s"Failed to start myndla-api: ${ex.getMessage}")
          ex.printStackTrace()
        })
    }: Unit
    articleApi = new articleapi.MainClass(articleApiProperties)
    Future {
      val result = articleApi.run(Array.empty)
      result
        .failed
        .foreach(ex => {
          println(s"Failed to start article-api: ${ex.getMessage}")
          ex.printStackTrace()
        })
    }: Unit

    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(any)).thenReturn(Success("feideid"))
    when(myndlaApi.componentRegistry.feideApiClient.getFeideExtendedUser(any)).thenReturn(
      Success(FeideExtendedUserInfo("", Seq("employee"), Some("employee"), "email@ndla.no", Some(Seq("email@ndla.no"))))
    )
    when(myndlaApi.componentRegistry.feideApiClient.getOrganization(any)).thenReturn(Success("org"))
    when(myndlaApi.componentRegistry.feideApiClient.getFeideGroups(any)).thenReturn(Success(Seq.empty))

    blockUntilHealthy(s"$myndlaApiBaseUrl/health/readiness")
    blockUntilHealthy(s"$articleApiBaseUrl/health/readiness")
  }

  override def afterAll(): Unit = {
    super.afterAll()
  }

  val feideToken: String = "valid-feide-token"

  class LocalArticleApiTestData {
    implicit lazy val props: ArticleApiProperties = articleApiProperties
    val td                                        = new ArticleTestData

    def setupArticles(): Try[Boolean] = (
      1L to 10
    ).map(id => {
        articleApi
          .componentRegistry
          .articleRepository
          .updateArticleFromDraftApi(
            td.sampleDomainArticle
              .copy(
                id = Some(id),
                updated = NDLADate.fromUnixTime(0),
                created = NDLADate.fromUnixTime(0),
                published = NDLADate.fromUnixTime(0),
              ),
            List(s"1$id"),
          )(using articleApi.componentRegistry.dbUtility.autoSession)
      })
      .collectFirst { case Failure(ex) =>
        Failure(ex)
      }
      .getOrElse(Success(true))
  }

  val dataFixer = new LocalArticleApiTestData

  test("teacher-only article lookup triggers myndla /intern/get-user via FeideAuthorization") {
    val article = dataFixer
      .td
      .sampleDomainArticle
      .copy(
        id = Some(123L),
        updated = NDLADate.fromUnixTime(0),
        created = NDLADate.fromUnixTime(0),
        published = NDLADate.fromUnixTime(0),
        availability = Availability.teacher,
      )
    val insertResult = articleApi
      .componentRegistry
      .articleRepository
      .updateArticleFromDraftApi(article, List("123"))(using articleApi.componentRegistry.dbUtility.autoSession)

    insertResult.isSuccess should be(true)

    val response = simpleHttpClient.send(
      quickRequest
        .get(uri"$articleApiBaseUrl/article-api/v2/articles/123")
        .header("FeideAuthorization", s"Bearer $feideToken")
    )

    response.isSuccess should be(true)
  }
}
