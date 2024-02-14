/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import enumeratum.Json4s
import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.article.Article
import no.ndla.common.model.domain.{ArticleType, Author, Availability}
import org.json4s.ext.{EnumNameSerializer, JavaTimeSerializers}
import org.json4s.{DefaultFormats, Formats}
import org.mockito.invocation.InvocationOnMock
import sttp.client3.quick._

import scala.util.{Failure, Success}

class InternControllerTest extends UnitSuite with TestEnvironment {
  implicit val formats: Formats =
    DefaultFormats +
      new EnumNameSerializer(Availability) ++
      JavaTimeSerializers.all +
      Json4s.serializer(ArticleType) +
      NDLADate.Json4sSerializer

  val author = Author("forfatter", "Henrik")

  val controller = new InternController

  val serverPort: Int = findFreePort
  when(clock.now()).thenCallRealMethod()

  override val services = List(controller)

  override def beforeAll(): Unit = {
    IO { Routes.startJdkServer(this.getClass.getName, serverPort) {} }.unsafeRunAndForget()
    Thread.sleep(1000)
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
    when(contentValidator.validateArticle(any[Article], any))
      .thenReturn(Success(TestData.sampleArticleWithByNcSa))

    import io.circe.generic.auto._
    import io.circe.syntax._
    val jsonStr = TestData.sampleArticleWithByNcSa.asJson.deepDropNullValues.noSpaces

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

  test("that update article arguments are parsed correctly") {
    reset(writeService)
    when(writeService.updateArticle(any, any, any, any)).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[Article](0))
    )
    val authHeaderWithWriteRole =
      "Bearer eyJhbGciOiJIUzI1NiJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiYXpwIjoiMTIzIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoiYXVkaW86d3JpdGUiLCJndHkiOiJjbGllbnQtY3JlZGVudGlhbHMiLCJwZXJtaXNzaW9ucyI6WyJhdWRpbzp3cml0ZSIsImFydGljbGVzOnB1Ymxpc2giLCJhcnRpY2xlczp3cml0ZSIsImNvbmNlcHQ6YWRtaW4iLCJjb25jZXB0OndyaXRlIiwiZHJhZnRzOmFkbWluIiwiZHJhZnRzOmh0bWwiLCJkcmFmdHM6cHVibGlzaCIsImRyYWZ0czp3cml0ZSIsImZyb250cGFnZTphZG1pbiIsImZyb250cGFnZTp3cml0ZSIsImltYWdlczp3cml0ZSIsImxlYXJuaW5ncGF0aDphZG1pbiIsImxlYXJuaW5ncGF0aDpwdWJsaXNoIiwibGVhcm5pbmdwYXRoOndyaXRlIl19.nm77NIe8aFACafNhC1nROU1bTspbT-hCvxlg6_8ztDk"

    import io.circe.generic.auto._
    import io.circe.syntax._
    val art     = TestData.sampleArticleWithByNcSa.copy(id = Some(10L))
    val jsonStr = art.asJson.deepDropNullValues.noSpaces

    val response = simpleHttpClient.send(
      quickRequest
        .post(uri"http://localhost:$serverPort/intern/article/10?external-id=")
        .headers(Map("Authorization" -> authHeaderWithWriteRole))
        .body(jsonStr)
    )
    response.code.code should be(200)

    verify(writeService, times(1)).updateArticle(
      article = eqTo(art),
      externalIds = eqTo(List.empty),
      useImportValidation = eqTo(false),
      useSoftValidation = eqTo(false)
    )
  }

  test("That no endpoints are shadowed") {
    import sttp.tapir.testing.EndpointVerifier
    val errors = EndpointVerifier(controller.endpoints.map(_.endpoint))
    if (errors.nonEmpty) {
      val errString = errors.map(e => e.toString).mkString("\n\t- ", "\n\t- ", "")
      fail(s"Got errors when verifying ${controller.serviceName} controller:$errString")
    }
  }

}
