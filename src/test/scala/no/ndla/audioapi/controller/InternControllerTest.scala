/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.model.domain.{AudioMetaInformation, AudioType}
import no.ndla.audioapi.model.{api, domain}
import no.ndla.audioapi.{AudioApiProperties, TestEnvironment, UnitSuite}
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatra.test.scalatest.ScalatraSuite

import java.util.Date
import scala.util.{Failure, Success}

class InternControllerTest extends UnitSuite with ScalatraSuite with TestEnvironment {

  override val converterService = new ConverterService
  lazy val controller = new InternController
  addServlet(controller, "/*")

  val updated: Date = new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC).toDate

  val DefaultApiImageMetaInformation: api.AudioMetaInformation = api.AudioMetaInformation(
    1,
    1,
    api.Title("title", "nb"),
    api.Audio("audio/test.mp3", "audio/mpeg", 1024, "nb"),
    api.Copyright(api.License("by-sa", None, None), None, Seq(), Seq(), Seq(), None, None, None),
    api.Tag(Seq("tag"), "nb"),
    Seq("nb"),
    "standard",
    None,
    None
  )

  val DefaultDomainImageMetaInformation: AudioMetaInformation = domain.AudioMetaInformation(
    Some(1),
    Some(1),
    Seq(domain.Title("title", "nb")),
    Seq(domain.Audio("audio/test.mp3", "audio/mpeg", 1024, "nb")),
    domain.Copyright("by-sa", None, Seq(), Seq(), Seq(), None, None, None),
    Seq(domain.Tag(Seq("tag"), "nb")),
    "ndla124",
    updated,
    Seq.empty,
    AudioType.Standard,
    Seq.empty,
    None
  )

  val DefaultDomainAudioNoLanguage: AudioMetaInformation = domain.AudioMetaInformation(
    Some(1),
    Some(1),
    Seq(domain.Title("title", "unknown")),
    Seq(domain.Audio("audio/test.mp3", "audio/mpeg", 1024, "unknown")),
    domain.Copyright("by-sa", None, Seq(), Seq(), Seq(), None, None, None),
    Seq(domain.Tag(Seq("tag"), "unknown")),
    "ndla124",
    updated,
    Seq.empty,
    AudioType.Standard,
    Seq.empty,
    None
  )

  test("That POST /import/123 returns 200 OK when import is a success") {
    when(importService.importAudio(eqTo("123"))).thenReturn(Success(DefaultDomainImageMetaInformation))
    when(audioIndexService.indexDocument(eqTo(DefaultDomainImageMetaInformation)))
      .thenReturn(Success(DefaultDomainImageMetaInformation))
    when(tagIndexService.indexDocument(eqTo(DefaultDomainImageMetaInformation)))
      .thenReturn(Success(DefaultDomainImageMetaInformation))
    post("/import/123") {
      status should equal(200)
    }
  }

  test("That POST /import/123 returns 200 OK when imported resource does not have a language") {
    when(importService.importAudio(eqTo("123"))).thenReturn(Success(DefaultDomainAudioNoLanguage))
    when(audioIndexService.indexDocument(eqTo(DefaultDomainAudioNoLanguage)))
      .thenReturn(Success(DefaultDomainAudioNoLanguage))
    when(tagIndexService.indexDocument(eqTo(DefaultDomainAudioNoLanguage)))
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
    reset(audioIndexService)
    when(audioIndexService.findAllIndexes(any[String])).thenReturn(Success(List("index1", "index2", "index3")))
    doReturn(Success(""), Nil: _*).when(audioIndexService).deleteIndexWithName(Some("index1"))
    doReturn(Success(""), Nil: _*).when(audioIndexService).deleteIndexWithName(Some("index2"))
    doReturn(Success(""), Nil: _*).when(audioIndexService).deleteIndexWithName(Some("index3"))
    delete("/index") {
      status should equal(200)
      body should equal("Deleted 3 indexes")
    }
    verify(audioIndexService).findAllIndexes(AudioApiProperties.SearchIndex)
    verify(audioIndexService).deleteIndexWithName(Some("index1"))
    verify(audioIndexService).deleteIndexWithName(Some("index2"))
    verify(audioIndexService).deleteIndexWithName(Some("index3"))
    verifyNoMoreInteractions(audioIndexService)
  }

  test("That DELETE /index fails if at least one index isn't found, and no indexes are deleted") {
    reset(audioIndexService)
    doReturn(Failure(new RuntimeException("Failed to find indexes")), Nil: _*)
      .when(audioIndexService)
      .findAllIndexes(AudioApiProperties.SearchIndex)
    doReturn(Success(""), Nil: _*).when(audioIndexService).deleteIndexWithName(Some("index1"))
    doReturn(Success(""), Nil: _*).when(audioIndexService).deleteIndexWithName(Some("index2"))
    doReturn(Success(""), Nil: _*).when(audioIndexService).deleteIndexWithName(Some("index3"))
    delete("/index") {
      status should equal(500)
      body should equal("Failed to find indexes")
    }
    verify(audioIndexService, never).deleteIndexWithName(any[Option[String]])
  }

  test(
    "That DELETE /index fails if at least one index couldn't be deleted, but the other indexes are deleted regardless") {
    reset(audioIndexService)
    when(audioIndexService.findAllIndexes(any[String])).thenReturn(Success(List("index1", "index2", "index3")))
    doReturn(Success(""), Nil: _*).when(audioIndexService).deleteIndexWithName(Some("index1"))
    doReturn(Failure(new RuntimeException("No index with name 'index2' exists")), Nil: _*)
      .when(audioIndexService)
      .deleteIndexWithName(Some("index2"))
    doReturn(Success(""), Nil: _*).when(audioIndexService).deleteIndexWithName(Some("index3"))
    delete("/index") {
      status should equal(500)
      body should equal(
        "Failed to delete 1 index: No index with name 'index2' exists. 2 indexes were deleted successfully.")
    }
    verify(audioIndexService).deleteIndexWithName(Some("index1"))
    verify(audioIndexService).deleteIndexWithName(Some("index2"))
    verify(audioIndexService).deleteIndexWithName(Some("index3"))
  }

}
