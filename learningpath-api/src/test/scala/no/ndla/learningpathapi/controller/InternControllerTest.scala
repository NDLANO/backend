/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.controller

import no.ndla.learningpathapi.{Eff, TestEnvironment, UnitSuite}
import no.ndla.tapirtesting.TapirControllerTest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{doReturn, never, reset, verify, verifyNoMoreInteractions, when}
import scalikejdbc.DBSession
import sttp.client3.quick.*

import scala.util.{Failure, Success}

class InternControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest[Eff] {
  val controller: InternController = new InternController

  test("that id with value 404 gives OK") {
    resetMocks()
    when(learningPathRepository.getIdFromExternalId(any[String])(any[DBSession])).thenReturn(Some(404L))

    simpleHttpClient
      .send(
        quickRequest.get(uri"http://localhost:$serverPort/intern/id/1234")
      )
      .code
      .code should be(200)
  }

  test("That DELETE /index removes all indexes") {
    reset(searchIndexService)
    when(searchIndexService.findAllIndexes(any[String])).thenReturn(Success(List("index1", "index2", "index3")))
    doReturn(Success(""), Nil: _*).when(searchIndexService).deleteIndexWithName(Some("index1"))
    doReturn(Success(""), Nil: _*).when(searchIndexService).deleteIndexWithName(Some("index2"))
    doReturn(Success(""), Nil: _*).when(searchIndexService).deleteIndexWithName(Some("index3"))

    val res = simpleHttpClient.send(
      quickRequest.delete(uri"http://localhost:$serverPort/intern/index")
    )
    res.code.code should be(200)
    res.body should be("Deleted 3 indexes")
    verify(searchIndexService).findAllIndexes(props.SearchIndex)
    verify(searchIndexService).deleteIndexWithName(Some("index1"))
    verify(searchIndexService).deleteIndexWithName(Some("index2"))
    verify(searchIndexService).deleteIndexWithName(Some("index3"))
    verifyNoMoreInteractions(searchIndexService)
  }

  test("That DELETE /index fails if at least one index isn't found, and no indexes are deleted") {
    reset(searchIndexService)
    doReturn(Failure(new RuntimeException("Failed to find indexes")), Nil: _*)
      .when(searchIndexService)
      .findAllIndexes(props.SearchIndex)
    doReturn(Success(""), Nil: _*).when(searchIndexService).deleteIndexWithName(Some("index1"))
    doReturn(Success(""), Nil: _*).when(searchIndexService).deleteIndexWithName(Some("index2"))
    doReturn(Success(""), Nil: _*).when(searchIndexService).deleteIndexWithName(Some("index3"))
    val res = simpleHttpClient.send(
      quickRequest.delete(uri"http://localhost:$serverPort/intern/index")
    )
    res.code.code should be(500)
    res.body should equal("Failed to find indexes")
    verify(searchIndexService, never).deleteIndexWithName(any[Option[String]])
  }

  test(
    "That DELETE /index fails if at least one index couldn't be deleted, but the other indexes are deleted regardless"
  ) {
    reset(searchIndexService)
    when(searchIndexService.findAllIndexes(any[String])).thenReturn(Success(List("index1", "index2", "index3")))
    doReturn(Success(""), Nil: _*).when(searchIndexService).deleteIndexWithName(Some("index1"))
    doReturn(Failure(new RuntimeException("No index with name 'index2' exists")), Nil: _*)
      .when(searchIndexService)
      .deleteIndexWithName(Some("index2"))
    doReturn(Success(""), Nil: _*).when(searchIndexService).deleteIndexWithName(Some("index3"))
    val res = simpleHttpClient.send(
      quickRequest.delete(uri"http://localhost:$serverPort/intern/index")
    )
    res.code.code should be(500)
    res.body should be(
      "Failed to delete 1 index: No index with name 'index2' exists. 2 indexes were deleted successfully."
    )
    verify(searchIndexService).deleteIndexWithName(Some("index1"))
    verify(searchIndexService).deleteIndexWithName(Some("index2"))
    verify(searchIndexService).deleteIndexWithName(Some("index3"))
  }
}
