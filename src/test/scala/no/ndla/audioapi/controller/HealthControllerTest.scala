/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.{TestEnvironment, UnitSuite}
import org.scalatra.test.scalatest.ScalatraFunSuite
import org.mockito.Mockito._
import scalaj.http.HttpResponse

class HealthControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {

  val audioSearchBody = s"""{
                           |	"totalCount": 1,
                           |	"page": 1,
                           |	"pageSize": 10,
                           |	"language": "unknown",
                           |	"results": [
                           |		{
                           |			"id": 5,
                           |			"title": {
                           |				"title": "Min bit av himmelen",
                           |				"language": "unknown"
                           |			},
                           |			"url": "http://0.0.0.0/audio-api/v1/audio/5",
                           |			"license": "by-sa",
                           |			"supportedLanguages": [
                           |				"unknown"
                           |			]
                           |		}
                           |  ]
                           |}""".stripMargin
  val audioResultBody = """{
                          |	"id": 5,
                          |	"revision": 2,
                          |	"title": {
                          |		"title": "Min bit av himmelen",
                          |		"language": "unknown"
                          |	},
                          |	"audioFile": {
                          |		"url": "http://0.0.0.0/audio/files/min_bit_av_himmelen_klipp.mp3",
                          |		"mimeType": "audio/mpeg",
                          |		"fileSize": 7426944,
                          |		"language": "unknown"
                          |	},
                          |	"copyright": {
                          |		"license": {
                          |			"license": "by-sa",
                          |			"description": "Creative Commons Attribution-ShareAlike 2.0 Generic",
                          |			"url": "https://creativecommons.org/licenses/by-sa/2.0/"
                          |		},
                          |		"creators": [
                          |			{
                          |				"type": "Writer",
                          |				"name": "Sheida Jahanbin"
                          |			}
                          |		],
                          |		"processors": [],
                          |		"rightsholders": [
                          |			{
                          |				"type": "Supplier",
                          |				"name": "NRK"
                          |			}
                          |		]
                          |	},
                          |	"tags": {
                          |		"tags": [
                          |			"radio documentary",
                          |			"radio journalist",
                          |			"sheida jahanbin"
                          |		],
                          |		"language": "unknown"
                          |	},
                          |	"supportedLanguages": [
                          |		"unknown"
                          |	]
                          |}""".stripMargin
  val httpResponseMock: HttpResponse[String] = mock[HttpResponse[String]]

  lazy val controller = new HealthController {
    override def getApiResponse(url: String): HttpResponse[String] = httpResponseMock
  }
  addServlet(controller, "/")

  test("that url is fetched properly") {
    val expectedUrl = "http://0.0.0.0/audio-api/v1/audio/5"
    val (url, totalCount) = controller.getAudioUrl(audioSearchBody)

    url should equal(Some(expectedUrl))
    totalCount should equal(1)
  }

  test("that /health returns 200 on success") {
    when(httpResponseMock.code).thenReturn(200)
    when(httpResponseMock.body).thenReturn(audioSearchBody).thenReturn(audioResultBody)

    get("/") {
      status should equal (200)
    }
  }

  test("that /health returns 500 on failure") {
    when(httpResponseMock.code).thenReturn(500)
    when(httpResponseMock.body).thenReturn(audioSearchBody).thenReturn(audioResultBody)

    get("/") {
      status should equal(500)
    }
  }

  test("that /health returns 200 on no images") {
    val noImageBody = s"""{
                         |	"totalCount": 0,
                         |	"page": 1,
                         |	"pageSize": 10,
                         |  "language": "unknown",
                         |  "results": []
                         |}""".stripMargin
    val notFoundBody = s"""{
                          |	"code": "NOT_FOUND",
                          |	"description": "Audio with id 51231 not found",
                          |	"occuredAt": "2017-11-24T14:00:22Z"
                          |}""".stripMargin
    when(httpResponseMock.code).thenReturn(200).thenReturn(404)
    when(httpResponseMock.body).thenReturn(noImageBody).thenReturn(notFoundBody)

    get("/") {
      status should equal(200)
    }
  }

}
