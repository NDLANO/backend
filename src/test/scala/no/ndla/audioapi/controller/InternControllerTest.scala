/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.model.{api, domain}
import no.ndla.audioapi.{AudioApiProperties, TestEnvironment, UnitSuite}
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatra.test.scalatest.ScalatraSuite

import scala.util.{Failure, Success}

class InternControllerTest extends UnitSuite with ScalatraSuite with TestEnvironment {

  override val converterService = new ConverterService
  lazy val controller = new InternController
  addServlet(controller, "/*")

  val updated = new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC).toDate

  val DefaultApiImageMetaInformation = api.AudioMetaInformation(
    1,
    1,
    api.Title("title", "nb"),
    api.Audio("audio/test.mp3", "audio/mpeg", 1024, "nb"),
    api.Copyright(api.License("by-sa", None, None), None, Seq(), Seq(), Seq(), None, None, None),
    api.Tag(Seq("tag"), "nb"),
    Seq("nb")
  )

  val DefaultDomainImageMetaInformation = domain.AudioMetaInformation(
    Some(1),
    Some(1),
    Seq(domain.Title("title", "nb")),
    Seq(domain.Audio("audio/test.mp3", "audio/mpeg", 1024, "nb")),
    domain.Copyright("by-sa", None, Seq(), Seq(), Seq(), None, None, None),
    Seq(domain.Tag(Seq("tag"), "nb")),
    "ndla124",
    updated
  )

  val DefaultDomainAudioNoLanguage = domain.AudioMetaInformation(
    Some(1),
    Some(1),
    Seq(domain.Title("title", "unknown")),
    Seq(domain.Audio("audio/test.mp3", "audio/mpeg", 1024, "unknown")),
    domain.Copyright("by-sa", None, Seq(), Seq(), Seq(), None, None, None),
    Seq(domain.Tag(Seq("tag"), "unknown")),
    "ndla124",
    updated
  )

  test("That POST /import/123 returns 200 OK when import is a success") {
    when(importService.importAudio(eqTo("123"))).thenReturn(Success(DefaultDomainImageMetaInformation))
    when(searchIndexService.indexDocument(eqTo(DefaultDomainImageMetaInformation)))
      .thenReturn(Success(DefaultDomainImageMetaInformation))
    post("/import/123") {
      status should equal(200)
    }
  }

  test("That POST /import/123 returns 200 OK when imported resource does not have a language") {
    when(importService.importAudio(eqTo("123"))).thenReturn(Success(DefaultDomainAudioNoLanguage))
    when(searchIndexService.indexDocument(eqTo(DefaultDomainAudioNoLanguage)))
      .thenReturn(Success(DefaultDomainAudioNoLanguage))

    post("/import/123") {
      status should equal(200)
    }
  }

  test("That POST /import/123 returns 500 with error message when import failed") {
    when(importService.importAudio(eqTo("123")))
      .thenReturn(Failure(new NullPointerException("There was a nullpointer exception")))

    post("/import/123") {
      status should equal(500)
      body indexOf "nullpointer" should be > 0
    }
  }

  test("That DELETE /index removes all indexes") {
    reset(indexService)
    when(indexService.findAllIndexes(any[String])).thenReturn(Success(List("index1", "index2", "index3")))
    doReturn(Success(""), Nil: _*).when(indexService).deleteIndexWithName(Some("index1"))
    doReturn(Success(""), Nil: _*).when(indexService).deleteIndexWithName(Some("index2"))
    doReturn(Success(""), Nil: _*).when(indexService).deleteIndexWithName(Some("index3"))
    delete("/index") {
      status should equal(200)
      body should equal("Deleted 3 indexes")
    }
    verify(indexService).findAllIndexes(AudioApiProperties.SearchIndex)
    verify(indexService).deleteIndexWithName(Some("index1"))
    verify(indexService).deleteIndexWithName(Some("index2"))
    verify(indexService).deleteIndexWithName(Some("index3"))
    verifyNoMoreInteractions(indexService)
  }

  test("That DELETE /index fails if at least one index isn't found, and no indexes are deleted") {
    reset(indexService)
    doReturn(Failure(new RuntimeException("Failed to find indexes")), Nil: _*)
      .when(indexService)
      .findAllIndexes(AudioApiProperties.SearchIndex)
    doReturn(Success(""), Nil: _*).when(indexService).deleteIndexWithName(Some("index1"))
    doReturn(Success(""), Nil: _*).when(indexService).deleteIndexWithName(Some("index2"))
    doReturn(Success(""), Nil: _*).when(indexService).deleteIndexWithName(Some("index3"))
    delete("/index") {
      status should equal(500)
      body should equal("Failed to find indexes")
    }
    verify(indexService, never()).deleteIndexWithName(any[Option[String]])
  }

  test(
    "That DELETE /index fails if at least one index couldn't be deleted, but the other indexes are deleted regardless") {
    reset(indexService)
    when(indexService.findAllIndexes(any[String])).thenReturn(Success(List("index1", "index2", "index3")))
    doReturn(Success(""), Nil: _*).when(indexService).deleteIndexWithName(Some("index1"))
    doReturn(Failure(new RuntimeException("No index with name 'index2' exists")), Nil: _*)
      .when(indexService)
      .deleteIndexWithName(Some("index2"))
    doReturn(Success(""), Nil: _*).when(indexService).deleteIndexWithName(Some("index3"))
    delete("/index") {
      status should equal(500)
      body should equal(
        "Failed to delete 1 index: No index with name 'index2' exists. 2 indexes were deleted successfully.")
    }
    verify(indexService).deleteIndexWithName(Some("index1"))
    verify(indexService).deleteIndexWithName(Some("index2"))
    verify(indexService).deleteIndexWithName(Some("index3"))
  }

}
