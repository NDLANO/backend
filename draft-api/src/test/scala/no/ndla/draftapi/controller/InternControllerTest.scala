/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.controller

import no.ndla.draftapi._
import no.ndla.draftapi.model.api.ContentId
import no.ndla.draftapi.model.domain.ImportId
import no.ndla.tapirtesting.TapirControllerTest
import org.json4s.Formats
import sttp.client3.quick._

import scala.util.{Failure, Success}

class InternControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest[Eff] {
  implicit val formats: Formats = org.json4s.DefaultFormats

  val controller = new InternController

  override def beforeEach(): Unit = {
    reset(clock)
    when(clock.now()).thenCallRealMethod()
  }

  test("that deleting an article goes several attempts if call to article-api fails") {
    val failedApiCall = Failure(new RuntimeException("Api call failed :/"))

    when(articleApiClient.deleteArticle(any[Long], any)).thenReturn(
      failedApiCall,
      failedApiCall,
      failedApiCall,
      Success(ContentId(10))
    )

    simpleHttpClient.send(
      quickRequest
        .delete(uri"http://localhost:$serverPort/intern/article/10/")
        .headers(Map("Authorization" -> TestData.authHeaderWithWriteRole))
    )
    verify(articleApiClient, times(4)).deleteArticle(eqTo(10), any)
  }

  test("that getting ids returns 404 for missing and 200 for existing") {
    val uuid = "16d4668f-0917-488b-9b4a-8f7be33bb72a"

    when(readService.importIdOfArticle("1234")).thenReturn(None)

    {
      val res = simpleHttpClient.send(
        quickRequest
          .get(uri"http://localhost:$serverPort/intern/import-id/1234")
      )
      res.code.code should be(404)
    }

    when(readService.importIdOfArticle("1234")).thenReturn(Some(ImportId(Some(uuid))))

    {
      val res = simpleHttpClient.send(
        quickRequest
          .get(uri"http://localhost:$serverPort/intern/import-id/1234")
      )
      res.code.code should be(200)
      res.body should be(s"""{"importId":"$uuid"}""".stripMargin)
    }
  }

  test("That DELETE /index removes all indexes") {
    reset(
      articleIndexService,
      tagIndexService,
      grepCodesIndexService
    )

    when(articleIndexService.findAllIndexes(any[String])).thenReturn(Success(List("index1", "index2")))
    when(tagIndexService.findAllIndexes(any[String])).thenReturn(Success(List("index7", "index8")))
    when(grepCodesIndexService.findAllIndexes(any[String])).thenReturn(Success(List("index9", "index10")))
    doReturn(Success(""), Nil: _*).when(articleIndexService).deleteIndexWithName(Some("index1"))
    doReturn(Success(""), Nil: _*).when(articleIndexService).deleteIndexWithName(Some("index2"))
    doReturn(Success(""), Nil: _*).when(tagIndexService).deleteIndexWithName(Some("index7"))
    doReturn(Success(""), Nil: _*).when(tagIndexService).deleteIndexWithName(Some("index8"))
    doReturn(Success(""), Nil: _*).when(grepCodesIndexService).deleteIndexWithName(Some("index9"))
    doReturn(Success(""), Nil: _*).when(grepCodesIndexService).deleteIndexWithName(Some("index10"))

    {
      val res = simpleHttpClient.send(
        quickRequest
          .delete(uri"http://localhost:$serverPort/intern/index")
      )
      res.code.code should be(200)
      res.body should equal("Deleted 6 indexes")
    }

    verify(articleIndexService).findAllIndexes(props.DraftSearchIndex)
    verify(articleIndexService).deleteIndexWithName(Some("index1"))
    verify(articleIndexService).deleteIndexWithName(Some("index2"))
    verifyNoMoreInteractions(articleIndexService)

    verify(tagIndexService).findAllIndexes(props.DraftTagSearchIndex)
    verify(tagIndexService).deleteIndexWithName(Some("index7"))
    verify(tagIndexService).deleteIndexWithName(Some("index8"))
    verifyNoMoreInteractions(tagIndexService)

    verify(grepCodesIndexService).findAllIndexes(props.DraftGrepCodesSearchIndex)
    verify(grepCodesIndexService).deleteIndexWithName(Some("index9"))
    verify(grepCodesIndexService).deleteIndexWithName(Some("index10"))
    verifyNoMoreInteractions(grepCodesIndexService)
  }

  test("That DELETE /index fails if at least one index isn't found, and no indexes are deleted") {
    reset(
      articleIndexService,
      tagIndexService,
      grepCodesIndexService
    )

    doReturn(Failure(new RuntimeException("Failed to find indexes")), Nil: _*)
      .when(articleIndexService)
      .findAllIndexes(props.DraftSearchIndex)
    doReturn(Success(""), Nil: _*).when(articleIndexService).deleteIndexWithName(Some("index1"))
    doReturn(Success(""), Nil: _*).when(articleIndexService).deleteIndexWithName(Some("index2"))

    {
      val res = simpleHttpClient.send(
        quickRequest
          .delete(uri"http://localhost:$serverPort/intern/index")
      )
      res.code.code should be(500)
      res.body should equal("Failed to find indexes")
    }

    verify(articleIndexService, never).deleteIndexWithName(any[Option[String]])
  }

  test(
    "That DELETE /index fails if at least one index couldn't be deleted, but the other indexes are deleted regardless"
  ) {
    reset(
      articleIndexService,
      tagIndexService,
      grepCodesIndexService
    )

    when(articleIndexService.findAllIndexes(any[String])).thenReturn(Success(List("index1", "index2")))
    when(tagIndexService.findAllIndexes(any[String])).thenReturn(Success(List("index7", "index8")))
    when(grepCodesIndexService.findAllIndexes(any[String])).thenReturn(Success(List("index9", "index10")))

    doReturn(Success(""), Nil: _*).when(articleIndexService).deleteIndexWithName(Some("index1"))
    doReturn(Failure(new RuntimeException("No index with name 'index2' exists")), Nil: _*)
      .when(articleIndexService)
      .deleteIndexWithName(Some("index2"))
    doReturn(Success(""), Nil: _*).when(tagIndexService).deleteIndexWithName(Some("index7"))
    doReturn(Success(""), Nil: _*).when(tagIndexService).deleteIndexWithName(Some("index8"))
    doReturn(Success(""), Nil: _*).when(grepCodesIndexService).deleteIndexWithName(Some("index9"))
    doReturn(Success(""), Nil: _*).when(grepCodesIndexService).deleteIndexWithName(Some("index10"))

    {
      val res = simpleHttpClient.send(
        quickRequest
          .delete(uri"http://localhost:$serverPort/intern/index")
      )
      res.code.code should be(500)
      res.body should equal(
        "Failed to delete 1 index: No index with name 'index2' exists. 5 indexes were deleted successfully."
      )
    }

    verify(articleIndexService).deleteIndexWithName(Some("index1"))
    verify(articleIndexService).deleteIndexWithName(Some("index2"))
    verify(tagIndexService).deleteIndexWithName(Some("index7"))
    verify(tagIndexService).deleteIndexWithName(Some("index8"))
    verify(grepCodesIndexService).deleteIndexWithName(Some("index9"))
    verify(grepCodesIndexService).deleteIndexWithName(Some("index10"))
  }
}
