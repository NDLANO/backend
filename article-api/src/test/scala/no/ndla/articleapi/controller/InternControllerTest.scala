/*
 * Part of NDLA article-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import no.ndla.common.model.domain.article.Article
import no.ndla.common.model.domain.Author
import no.ndla.tapirtesting.TapirControllerTest
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{doReturn, never, reset, times, verify, verifyNoMoreInteractions, when}
import org.mockito.invocation.InvocationOnMock
import sttp.client3.quick.*

import scala.util.{Failure, Success}

class InternControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest {
  val author: Author = Author("forfatter", "Henrik")

  val controller: TapirController = new InternController

  when(clock.now()).thenCallRealMethod()

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

    import io.circe.syntax.*
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
    doReturn(Success(""), Nil*).when(articleIndexService).deleteIndexWithName(Some("index1"))
    doReturn(Success(""), Nil*).when(articleIndexService).deleteIndexWithName(Some("index2"))
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

    doReturn(Failure(new RuntimeException("Failed to find indexes")), Nil*)
      .when(articleIndexService)
      .findAllIndexes(props.ArticleSearchIndex)
    doReturn(Success(""), Nil*).when(articleIndexService).deleteIndexWithName(Some("index1"))
    doReturn(Success(""), Nil*).when(articleIndexService).deleteIndexWithName(Some("index2"))
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

    doReturn(Success(""), Nil*).when(articleIndexService).deleteIndexWithName(Some("index1"))
    doReturn(Failure(new RuntimeException("No index with name 'index2' exists")), Nil*)
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
    when(writeService.updateArticle(any, any, any, any, any)(any)).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[Article](0))
    )
    val authHeaderWithWriteRole =
      "Bearer eyJhbGciOiJIUzI1NiJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiYXpwIjoiMTIzIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoiYXVkaW86d3JpdGUiLCJndHkiOiJjbGllbnQtY3JlZGVudGlhbHMiLCJwZXJtaXNzaW9ucyI6WyJhdWRpbzp3cml0ZSIsImFydGljbGVzOnB1Ymxpc2giLCJhcnRpY2xlczp3cml0ZSIsImNvbmNlcHQ6YWRtaW4iLCJjb25jZXB0OndyaXRlIiwiZHJhZnRzOmFkbWluIiwiZHJhZnRzOmh0bWwiLCJkcmFmdHM6cHVibGlzaCIsImRyYWZ0czp3cml0ZSIsImZyb250cGFnZTphZG1pbiIsImZyb250cGFnZTp3cml0ZSIsImltYWdlczp3cml0ZSIsImxlYXJuaW5ncGF0aDphZG1pbiIsImxlYXJuaW5ncGF0aDpwdWJsaXNoIiwibGVhcm5pbmdwYXRoOndyaXRlIl19.nm77NIe8aFACafNhC1nROU1bTspbT-hCvxlg6_8ztDk"

    import io.circe.syntax.*
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
      useSoftValidation = eqTo(false),
      skipValidation = eqTo(false)
    )(any)
  }

}
