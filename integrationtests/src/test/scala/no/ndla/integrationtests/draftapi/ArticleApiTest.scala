package no.ndla.integrationtests.draftapi

import no.ndla.articleapi
import no.ndla.scalatestsuite.{IntegrationSuite, UnitTestSuite}
import org.eclipse.jetty.server.Server
import scalaj.http.{Http, HttpResponse}

import java.util.Date

class ArticleApiTest extends IntegrationSuite(EnableElasticsearchContainer = true, EnablePostgresContainer = true) {

  val cont = postgresContainer.get

  val articleApiPort = 33333

  val testProps: articleapi.ArticleApiProperties = new articleapi.ArticleApiProperties {
    override def ApplicationPort: Int = articleApiPort
    override def MetaUserName: String = cont.getUsername
    override def MetaPassword: String = cont.getPassword
    override def MetaResource: String = cont.getDatabaseName
    override def MetaServer: String = cont.getContainerIpAddress
    override def MetaPort: Int = cont.getMappedPort(5432)
    override def MetaSchema: String = "article_api"
  }

  val articleApi = new articleapi.MainClass(testProps)
  val articleApiServer: Server = articleApi.startServer()

  test("some cool test") {

    val req = Http(s"localhost:${articleApiPort}/article-api/v2/articles/123123")
    val res = req.asString
    res match {
      case HttpResponse(body, code, headers) =>
        println("Code:", code)
        println("Body:", body)
    }
  }
}
