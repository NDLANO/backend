/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import no.ndla.audioapi.TestData._
import no.ndla.audioapi.model.domain
import no.ndla.audioapi.model.domain.{AudioMetaInformation, AudioType}
import no.ndla.audioapi.{TestEnvironment, UnitSuite}
import no.ndla.common.model.domain.article.Copyright
import no.ndla.common.model.{domain => common}
import sttp.client3.quick._

import scala.util.{Failure, Success}

class InternControllerTest extends UnitSuite with TestEnvironment {

  val serverPort: Int                           = findFreePort
  override val converterService                 = new ConverterService
  val controller                                = new InternController
  override val services: List[InternController] = List(controller)

  override def beforeAll(): Unit = {
    IO { Routes.startJdkServer("InternControllerTest", serverPort) {} }.unsafeRunAndForget()
    Thread.sleep(1000)
  }

  val DefaultDomainImageMetaInformation: AudioMetaInformation = domain.AudioMetaInformation(
    Some(1),
    Some(1),
    Seq(common.Title("title", "nb")),
    Seq(domain.Audio("audio/test.mp3", "audio/mpeg", 1024, "nb")),
    Copyright("by-sa", None, Seq(), Seq(), Seq(), None, None, false),
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
    Copyright("by-sa", None, Seq(), Seq(), Seq(), None, None, false),
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

    val request =
      quickRequest
        .delete(uri"http://localhost:$serverPort/intern/index")
    val response = simpleHttpClient.send(request)
    response.code.code should be(200)
    response.body should be("Deleted 3 indexes")

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

    val request =
      quickRequest
        .delete(uri"http://localhost:$serverPort/intern/index")
    val response = simpleHttpClient.send(request)
    response.code.code should be(500)
    response.body should be("Failed to find indexes")

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

    val request =
      quickRequest
        .delete(uri"http://localhost:$serverPort/intern/index")
    val response = simpleHttpClient.send(request)
    response.code.code should be(500)
    response.body should be(
      "Failed to delete 1 index: No index with name 'index2' exists. 2 indexes were deleted successfully."
    )

    verify(audioIndexService).deleteIndexWithName(Some("index1"))
    verify(audioIndexService).deleteIndexWithName(Some("index2"))
    verify(audioIndexService).deleteIndexWithName(Some("index3"))
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
