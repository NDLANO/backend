/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import enumeratum.Json4s
import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import no.ndla.common.model.domain.article.Article
import no.ndla.common.model.domain.{ArticleType, Author, Availability}
import no.ndla.network.tapir.TapirServer
import org.json4s.ext.{EnumNameSerializer, JavaTimeSerializers}
import org.json4s.{DefaultFormats, Formats}
import sttp.client3.quick._

import scala.util.{Failure, Success}

class InternControllerTest extends UnitSuite with TestEnvironment {
  implicit val formats: Formats =
    DefaultFormats + new EnumNameSerializer(Availability) ++ JavaTimeSerializers.all + Json4s.serializer(ArticleType)

  val author = Author("forfatter", "Henrik")

  val authHeaderWithWriteRole =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoiYXJ0aWNsZXM6d3JpdGUiLCJndHkiOiJjbGllbnQtY3JlZGVudGlhbHMifQ.kh82qM84FZgoo3odWbHTLWy-N049m7SyQw4gdatDMk43H2nWHA6gjsbJoiBIZ7BcbSfHElEZH0tP94vRy-kjgA3hflhOBbsD73DIxRvnbH1kSXlBnl6ISbgtHnzv1wQ7ShykMAcBsoWQ6J16ixK_p-msW42kcEqK1LanzPy-_qI"

  val controller = new InternController

  val serverPort: Int = findFreePort
  when(clock.now()).thenCallRealMethod()

  override def beforeAll(): Unit = {
    val app    = Routes.build(List(controller))
    val server = TapirServer(this.getClass.getName, serverPort, app, enableMelody = false)()
    server.runInBackground()
    blockUntil(() => server.isReady)
  }

  test("POST /validate/article should return 400 if the article is invalid") {
    val invalidArticle = """{"revision": 1, "title": [{"language": "nb", "titlee": "lol"]}"""

    val response = simpleHttpClient.send(
      quickRequest
        .post(
          uri"http://localhost:$serverPort/intern/validate/article"
        )
        .body(invalidArticle)
    )
    response.code.code should be(400)
  }

  test("POST /validate should return 204 if the article is valid") {
    when(contentValidator.validateArticle(any[Article], any[Boolean]))
      .thenReturn(Success(TestData.sampleArticleWithByNcSa))

    import io.circe.generic.auto._
    import io.circe.syntax._
    val jsonStr = TestData.sampleArticleWithByNcSa.asJson.noSpaces

    val response = simpleHttpClient.send(
      quickRequest
        .post(
          uri"http://localhost:$serverPort/intern/validate/article"
        )
        .body(jsonStr)
    )
    response.code.code should be(200)
  }

  test("That DELETE /index removes all indexes") {
    reset(articleIndexService)
    when(articleIndexService.findAllIndexes(any[String])).thenReturn(Success(List("index1", "index2")))
    doReturn(Success(""), Nil: _*).when(articleIndexService).deleteIndexWithName(Some("index1"))
    doReturn(Success(""), Nil: _*).when(articleIndexService).deleteIndexWithName(Some("index2"))
    val response = simpleHttpClient.send(
      quickRequest.delete(
        uri"http://localhost:$serverPort/intern/index"
      )
    )
    response.code.code should be(200)
    response.body should be("Deleted 2 indexes")

    verify(articleIndexService).findAllIndexes(props.ArticleSearchIndex)
    verify(articleIndexService).deleteIndexWithName(Some("index1"))
    verify(articleIndexService).deleteIndexWithName(Some("index2"))
    verifyNoMoreInteractions(articleIndexService)
  }

  test("That DELETE /index fails if at least one index isn't found, and no indexes are deleted") {
    reset(articleIndexService)

    doReturn(Failure(new RuntimeException("Failed to find indexes")), Nil: _*)
      .when(articleIndexService)
      .findAllIndexes(props.ArticleSearchIndex)
    doReturn(Success(""), Nil: _*).when(articleIndexService).deleteIndexWithName(Some("index1"))
    doReturn(Success(""), Nil: _*).when(articleIndexService).deleteIndexWithName(Some("index2"))
    val response = simpleHttpClient.send(
      quickRequest.delete(
        uri"http://localhost:$serverPort/intern/index"
      )
    )
    response.code.code should be(500)
    response.body should be("Failed to find indexes")

    verify(articleIndexService, never).deleteIndexWithName(any[Option[String]])
  }

  test(
    "That DELETE /index fails if at least one index couldn't be deleted, but the other indexes are deleted regardless"
  ) {
    reset(articleIndexService)

    when(articleIndexService.findAllIndexes(any[String])).thenReturn(Success(List("index1", "index2")))

    doReturn(Success(""), Nil: _*).when(articleIndexService).deleteIndexWithName(Some("index1"))
    doReturn(Failure(new RuntimeException("No index with name 'index2' exists")), Nil: _*)
      .when(articleIndexService)
      .deleteIndexWithName(Some("index2"))
    val response = simpleHttpClient.send(
      quickRequest.delete(
        uri"http://localhost:$serverPort/intern/index"
      )
    )
    response.code.code should be(500)
    response.body should be(
      "Failed to delete 1 index: No index with name 'index2' exists. 1 index were deleted successfully."
    )
    verify(articleIndexService).deleteIndexWithName(Some("index1"))
    verify(articleIndexService).deleteIndexWithName(Some("index2"))
  }

}
