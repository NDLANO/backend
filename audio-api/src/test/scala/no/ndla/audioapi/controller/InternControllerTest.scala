/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.model.domain.{AudioMetaInformation, AudioType}
import no.ndla.audioapi.model.{api, domain}
import no.ndla.audioapi.{TestEnvironment, UnitSuite}
import no.ndla.common.model.{domain => common}
import org.scalatra.test.scalatest.ScalatraSuite

import java.time.LocalDateTime
import scala.util.{Failure, Success}

class InternControllerTest extends UnitSuite with ScalatraSuite with TestEnvironment {

  override val converterService = new ConverterService
  lazy val controller           = new InternController
  addServlet(controller, "/*")

  val updated: LocalDateTime = LocalDateTime.of(2017, 4, 1, 12, 15, 32)
  val created: LocalDateTime = LocalDateTime.of(2017, 3, 1, 12, 15, 32)

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
    None,
    None,
    created,
    updated
  )

  val DefaultDomainImageMetaInformation: AudioMetaInformation = domain.AudioMetaInformation(
    Some(1),
    Some(1),
    Seq(common.Title("title", "nb")),
    Seq(domain.Audio("audio/test.mp3", "audio/mpeg", 1024, "nb")),
    domain.Copyright("by-sa", None, Seq(), Seq(), Seq(), None, None, None),
    Seq(common.Tag(Seq("tag"), "nb")),
    "ndla124",
    updated,
    created,
    Seq.empty,
    AudioType.Standard,
    Seq.empty,
    None,
    None
  )

  val DefaultDomainAudioNoLanguage: AudioMetaInformation = domain.AudioMetaInformation(
    Some(1),
    Some(1),
    Seq(common.Title("title", "unknown")),
    Seq(domain.Audio("audio/test.mp3", "audio/mpeg", 1024, "unknown")),
    domain.Copyright("by-sa", None, Seq(), Seq(), Seq(), None, None, None),
    Seq(common.Tag(Seq("tag"), "unknown")),
    "ndla124",
    updated,
    created,
    Seq.empty,
    AudioType.Standard,
    Seq.empty,
    None,
    None
  )

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
    verify(audioIndexService).findAllIndexes(props.SearchIndex)
    verify(audioIndexService).deleteIndexWithName(Some("index1"))
    verify(audioIndexService).deleteIndexWithName(Some("index2"))
    verify(audioIndexService).deleteIndexWithName(Some("index3"))
    verifyNoMoreInteractions(audioIndexService)
  }

  test("That DELETE /index fails if at least one index isn't found, and no indexes are deleted") {
    reset(audioIndexService)
    doReturn(Failure(new RuntimeException("Failed to find indexes")), Nil: _*)
      .when(audioIndexService)
      .findAllIndexes(props.SearchIndex)
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
    "That DELETE /index fails if at least one index couldn't be deleted, but the other indexes are deleted regardless"
  ) {
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
        "Failed to delete 1 index: No index with name 'index2' exists. 2 indexes were deleted successfully."
      )
    }
    verify(audioIndexService).deleteIndexWithName(Some("index1"))
    verify(audioIndexService).deleteIndexWithName(Some("index2"))
    verify(audioIndexService).deleteIndexWithName(Some("index3"))
  }

}
