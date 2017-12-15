/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.model.api._
import no.ndla.audioapi.{TestEnvironment, UnitSuite}
import org.scalatra.test.scalatest.ScalatraFunSuite
import org.mockito.Mockito._
import org.json4s.native.Serialization.write

import scalaj.http.HttpResponse

class HealthControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val formats = org.json4s.DefaultFormats

  val audioResultBody = AudioMetaInformation(
    id = 5,
    revision = 2,
    title = Title("Min bit av himmelen", "unknown"),
    audioFile = Audio(
      url = "http://0.0.0.0/audio/files/min_bit_av_himmelen_klipp.mp3",
      mimeType = "audio/mpeg",
      fileSize = 7426944,
      language = "unknown"
    ),
    copyright = Copyright(
      license = License("by-sa", Some("Creative Commons Attribution-ShareAlike 2.0 Generic"), Some("https://creativecommons.org/licenses/by-sa/2.0/")),
      origin = None,
      creators = List(Author("Writer", "Sheida Jahanbin")),
      processors = List(),
      rightsholders = List(Author("Supplier", "NRK")),
      agreementId = None,
      validFrom = None,
      validTo = None
    ),
    tags = Tag(List(
      "radio documentary",
      "radio journalist",
      "sheida jahanbin"
    ), "unknown"),
    supportedLanguages = List("unknown")
  )

  val audioSearchBody = SearchResult(
    totalCount = 1,
    page = 1,
    pageSize = 10,
    language = "unknown",
    results = List(
      AudioSummary(
        id = 5,
        title = Title("Min bit av himmelen", "unknown"),
        url = "http://0.0.0.0/audio-api/v1/audio/5",
        license = "by-sa",
        supportedLanguages = List(
          "unknown"
        )
      )

    )
  )

  val httpResponseMock: HttpResponse[String] = mock[HttpResponse[String]]

  lazy val controller = new HealthController {
    override def getApiResponse(url: String): HttpResponse[String] = httpResponseMock
  }
  addServlet(controller, "/")

  test("that url is fetched properly") {
    val expectedUrl = "http://0.0.0.0/audio-api/v1/audio/5"
    val (url, totalCount) = controller.getAudioUrl(write(audioSearchBody))

    url should equal(Some(expectedUrl))
    totalCount should equal(1)
  }

  test("that /health returns 200 on success") {
    when(httpResponseMock.code).thenReturn(200)
    when(httpResponseMock.body).thenReturn(write(audioSearchBody)).thenReturn(write(audioResultBody))

    get("/") {
      status should equal (200)
    }
  }

  test("that /health returns 500 on failure") {
    when(httpResponseMock.code).thenReturn(500)
    when(httpResponseMock.body).thenReturn(write(audioSearchBody)).thenReturn(write(audioResultBody))

    get("/") {
      status should equal(500)
    }
  }

  test("that /health returns 200 on no images") {
    val noImageBody = SearchResult(
      totalCount = 0,
      page = 1,
      pageSize = 10,
      language = "unknown",
      results = List()
    )
    val notFoundBody = s"""{
                          |	"code": "NOT_FOUND",
                          |	"description": "Audio with id 51231 not found",
                          |	"occuredAt": "2017-11-24T14:00:22Z"
                          |}""".stripMargin
    when(httpResponseMock.code).thenReturn(200).thenReturn(404)
    when(httpResponseMock.body).thenReturn(write(noImageBody)).thenReturn(notFoundBody)

    get("/") {
      status should equal(200)
    }
  }

}
