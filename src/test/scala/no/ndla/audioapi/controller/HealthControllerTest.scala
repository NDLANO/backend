/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.model.domain._
import no.ndla.audioapi.{TestEnvironment, UnitSuite}
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatra.test.scalatest.ScalatraFunSuite
import org.mockito.Mockito._
import org.json4s.native.Serialization.write

import scalaj.http.HttpResponse

class HealthControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val formats = org.json4s.DefaultFormats

  val httpResponseMock: HttpResponse[String] = mock[HttpResponse[String]]

  lazy val controller = new HealthController {
    override def getApiResponse(url: String): HttpResponse[String] = httpResponseMock
  }

  val updated = new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC).toDate

  val copyrighted =
    Copyright("copyrighted", Some("New York"), Seq(Author("Forfatter", "Clark Kent")), Seq(), Seq(), None, None, None)

  val audioMeta = AudioMetaInformation(
    Some(1),
    Some(1),
    Seq(Title("Batmen er på vift med en bil", "nb")),
    Seq(Audio("file.mp3", "audio/mpeg", 1024, "nb")),
    copyrighted,
    Seq(Tag(Seq("fisk"), "nb")),
    "ndla124",
    updated,
    Seq.empty,
    AudioType.Standard,
    Seq.empty
  )

  addServlet(controller, "/")
  when(httpResponseMock.code).thenReturn(404)
  when(audioRepository.getRandomAudio()).thenReturn(None)

  test("that /health returns 200 on success") {
    when(httpResponseMock.code).thenReturn(200)
    when(audioRepository.getRandomAudio()).thenReturn(Some(audioMeta))

    get("/") {
      status should equal(200)
    }
  }

  test("that /health returns 500 on failure") {
    when(httpResponseMock.code).thenReturn(500)
    when(audioRepository.getRandomAudio()).thenReturn(Some(audioMeta))

    get("/") {
      status should equal(500)
    }
  }

  test("that /health returns 200 on no images") {
    when(httpResponseMock.code).thenReturn(404)
    when(audioRepository.getRandomAudio()).thenReturn(None)

    get("/") {
      status should equal(200)
    }
  }

}
