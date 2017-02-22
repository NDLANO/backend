/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.model.api._
import no.ndla.audioapi.{AudioSwagger, TestEnvironment, UnitSuite}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatra.servlet.FileItem
import org.scalatra.test.Uploadable
import org.scalatra.test.scalatest.ScalatraSuite

import scala.util.{Failure, Success}

class AudioControllerTest extends UnitSuite with ScalatraSuite with TestEnvironment {

  implicit val swagger = new AudioSwagger
  lazy val controller = new AudioController
  addServlet(controller, "/*")

  val sampleUploadFile = new Uploadable {
    override def contentLength = 3
    override def content = Array[Byte](0x49, 0x44, 0x33)
    override def contentType = "audio/mp3"
    override def fileName = "test.mp3"
  }
  val sampleNewAudioMeta =
    """
      |{
      |    "titles": [{
      |        "title": "Test",
      |        "language": "nb"
      |    }],
      |    "audioFiles": [{
      |        "fileName": "test.mp3",
      |        "language": "nb"
      |    }],
      |    "copyright": {
      |        "license": {
      |            "license": "by-sa"
      |        },
      |        "authors": []
      |    },
      |    "tags": [{
      |        "tags": ["test"],
      |        "language": "nb"
      |    }]
      |}
    """.stripMargin

  test("That POST / returns 400 if parameters are missing") {
    post("/", ("metadata", sampleNewAudioMeta)) {
      status should equal (400)
    }
  }

  test("That POST / returns 200 if everything is fine and dandy") {
    val sampleAudioMeta = AudioMetaInformation(1, Seq(), Seq(), Copyright(License("by", None, None), None, Seq()), Seq())
    when(writeService.storeNewAudio(any[NewAudioMetaInformation], any[Seq[FileItem]])).thenReturn(Success(sampleAudioMeta))

    post("/", Map("metadata" -> sampleNewAudioMeta), Map("file" -> sampleUploadFile)) {
      status should equal (200)
    }
  }

  test("That POST / returns 500 if an unexpected error occurs") {
    when(writeService.storeNewAudio(any[NewAudioMetaInformation], any[Seq[FileItem]])).thenReturn(Failure(mock[RuntimeException]))

    post("/", Map("metadata" -> sampleNewAudioMeta), Map("file" -> sampleUploadFile)) {
      status should equal (500)
    }
  }

}
