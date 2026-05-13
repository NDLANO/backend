/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.controller

import no.ndla.common.{Clock, model, errors as common}
import no.ndla.common.model.api.FrontPageDTO
import no.ndla.frontpageapi.{TestEnvironment, UnitSuite}
import no.ndla.network.tapir.{ErrorHelpers, Routes, TapirController}
import no.ndla.scalatestsuite.NdlaAuthTestTokens
import no.ndla.tapirtesting.TapirControllerTest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import sttp.client4.quick.*

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

class PageControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest {
  override implicit lazy val clock: Clock                           = mock[Clock]
  override implicit lazy val errorHelpers: ErrorHelpers             = new ErrorHelpers
  override implicit lazy val errorHandling: ControllerErrorHandling = new ControllerErrorHandling
  val controller: FrontPageController                               = new FrontPageController
  override implicit lazy val services: List[TapirController]        = List(controller)
  override implicit lazy val routes: Routes                         = new Routes
  when(clock.now()).thenCallRealMethod()

  val authHeaderWithAdminRole = s"Bearer ${NdlaAuthTestTokens.FrontPageAdmin}"

  val authHeaderWithWrongRole = s"Bearer ${NdlaAuthTestTokens.LearningPathAdmin}"

  val authHeaderWithoutAnyRoles = s"Bearer ${NdlaAuthTestTokens.NoPermissions}"

  val malformedNewFrontPage: String = """{"malformed": "x"}"""

  val sampleNewFrontPage: String = """{
      |  "articleId": 15,
      |  "menu": [
      |    {
      |      "articleId": 1,
      |      "hideLevel": false,
      |      "menu": [
      |        {
      |          "articleId": 2,
      |          "hideLevel": false,
      |          "menu": [
      |            {
      |              "articleId": 4,
      |              "hideLevel": false,
      |              "menu": []
      |            }
      |          ]
      |        }
      |      ]
      |    },
      |    {
      |      "articleId": 3,
      |      "hideLevel": false,
      |      "menu": []
      |    }
      |  ]
      |}
    """.stripMargin

  test("That POST / returns 401 if no auth-header") {
    val request = quickRequest
      .post(uri"http://localhost:$serverPort/frontpage-api/v1/frontpage")
      .readTimeout(Duration.Inf)

    val response = request.send()
    response.code.code should be(401)
  }

  test("That POST / returns 403 if auth header does not have expected role") {
    val request = quickRequest
      .post(uri"http://localhost:$serverPort/frontpage-api/v1/frontpage")
      .readTimeout(Duration.Inf)
      .headers(Map("Authorization" -> authHeaderWithWrongRole))

    val response = request.send()
    response.code.code should be(403)
  }

  test("That POST / returns 403 if auth header does not have any roles") {
    val request = quickRequest
      .post(uri"http://localhost:$serverPort/frontpage-api/v1/frontpage")
      .readTimeout(Duration.Inf)
      .headers(Map("Authorization" -> authHeaderWithoutAnyRoles))

    val response = request.send()
    response.code.code should be(403)
  }

  test("That POST / returns 200 if auth header does have correct role") {
    when(writeService.createFrontPage(any)).thenReturn(Success(FrontPageDTO(1, List())))

    val request = quickRequest
      .post(uri"http://localhost:$serverPort/frontpage-api/v1/frontpage")
      .readTimeout(Duration.Inf)
      .headers(Map("Authorization" -> authHeaderWithAdminRole))
      .body(sampleNewFrontPage)

    val response = request.send()
    response.code.code should be(200)
  }

  test("That POST / returns 400 if auth header does have correct role but the json body is malformed") {
    when(writeService.createFrontPage(any)).thenReturn(Success(model.api.FrontPageDTO(1, List())))

    val request = quickRequest
      .post(uri"http://localhost:$serverPort/frontpage-api/v1/frontpage")
      .readTimeout(Duration.Inf)
      .headers(Map("Authorization" -> authHeaderWithAdminRole))
      .body(malformedNewFrontPage)

    val response = request.send()
    response.code.code should be(400)
  }

  test("That GET / returns 200 when the frontpage is available") {
    val frontPage = model.api.FrontPageDTO(articleId = 1, menu = List.empty)

    when(readService.getFrontPage).thenReturn(Success(frontPage))
    val request = quickRequest.get(uri"http://localhost:$serverPort/frontpage-api/v1/frontpage")

    val response = request.send()
    response.code.code should be(200)
  }

  test("That GET / returns 404 if frontpage is not found") {
    when(readService.getFrontPage).thenReturn(Failure(common.NotFoundException("Front page was not found")))
    val request = quickRequest.get(uri"http://localhost:$serverPort/frontpage-api/v1/frontpage")

    val response = request.send()
    response.code.code should be(404)
  }
}
