/*
 * Part of NDLA article-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import no.ndla.articleapi.service.ConverterService
import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import no.ndla.common.Clock
import no.ndla.common.model.domain.article.Article
import no.ndla.common.model.domain.{Author, ContributorType}
import no.ndla.network.tapir.{ErrorHandling, ErrorHelpers, Routes, TapirController}
import no.ndla.tapirtesting.{NdlaAuthTestTokens, TapirControllerTest}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.mockito.invocation.InvocationOnMock
import sttp.client4.quick.*

import scala.util.{Failure, Success, Try}

class InternControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest {
  val author: Author = Author(ContributorType.Writer, "Henrik")

  override implicit lazy val clock: Clock                       = mock[Clock]
  override implicit lazy val errorHelpers: ErrorHelpers         = new ErrorHelpers
  override implicit lazy val errorHandling: ErrorHandling       = new ControllerErrorHandling
  override implicit lazy val converterService: ConverterService = new ConverterService
  val controller: InternController                              = new InternController
  override implicit lazy val services: List[TapirController]    = List(controller)
  override implicit lazy val routes: Routes                     = new Routes

  override def beforeEach(): Unit = {
    super.beforeEach()

    when(clock.now()).thenCallRealMethod()
  }

  test("POST /validate/article should return 400 if the article is invalid") {
    val invalidArticle = """{"revision": 1, "title": [{"language": "nb", "titlee": "lol"]}"""

    val response = quickRequest
      .post(uri"http://localhost:$serverPort/intern/article-api/validate/article")
      .body(invalidArticle)
      .send()
    response.code.code should be(400)
  }

  test("POST /validate should return 204 if the article is valid") {
    when(contentValidator.validateArticle(any[Article], any)(using any)).thenReturn(
      Success(TestData.sampleArticleWithByNcSa)
    )

    import io.circe.syntax.*
    val jsonStr = TestData.sampleArticleWithByNcSa.asJson.deepDropNullValues.noSpaces

    val response = quickRequest
      .post(uri"http://localhost:$serverPort/intern/article-api/validate/article")
      .body(jsonStr)
      .send()
    response.code.code should be(200)
  }

  test("That DELETE /index removes all indexes") {
    reset(articleIndexService)
    when(articleIndexService.findAllIndexes(any[String])).thenReturn(Success(List("index1", "index2")))
    doReturn(Success(""), Nil*).when(articleIndexService).deleteIndexWithName(Some("index1"))
    doReturn(Success(""), Nil*).when(articleIndexService).deleteIndexWithName(Some("index2"))
    val response = quickRequest.delete(uri"http://localhost:$serverPort/intern/article-api/index").send()
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
    val response = quickRequest.delete(uri"http://localhost:$serverPort/intern/article-api/index").send()
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
    val response = quickRequest.delete(uri"http://localhost:$serverPort/intern/article-api/index").send()
    response.code.code should be(500)
    response.body should be(
      "Failed to delete 1 index: No index with name 'index2' exists. 1 index were deleted successfully."
    )
    verify(articleIndexService).deleteIndexWithName(Some("index1"))
    verify(articleIndexService).deleteIndexWithName(Some("index2"))
  }

  test("that update article arguments are parsed correctly") {
    reset(writeService)
    when(writeService.updateArticle(any, any, any, any)(any)).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[Article](0))
    )
    val authHeaderWithWriteRole = s"Bearer ${NdlaAuthTestTokens.ArticleWrite}"

    import io.circe.syntax.*
    val art     = TestData.sampleArticleWithByNcSa.copy(id = Some(10L))
    val jsonStr = art.asJson.deepDropNullValues.noSpaces

    val response = quickRequest
      .post(uri"http://localhost:$serverPort/intern/article-api/article/10?external-id=")
      .headers(Map("Authorization" -> authHeaderWithWriteRole))
      .body(jsonStr)
      .send()
    response.code.code should be(200)

    verify(writeService, times(1)).updateArticle(
      article = eqTo(art),
      useImportValidation = eqTo(false),
      useSoftValidation = eqTo(false),
      skipValidation = eqTo(false),
    )(any)
  }

}
