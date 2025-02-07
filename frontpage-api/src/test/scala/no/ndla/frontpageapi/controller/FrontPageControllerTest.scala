/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.controller

import no.ndla.common.{model, errors as common}
import no.ndla.common.model.api.FrontPageDTO
import no.ndla.frontpageapi.{TestEnvironment, UnitSuite}
import no.ndla.tapirtesting.TapirControllerTest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import sttp.client3.quick.*

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

class FrontPageControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest {
  val controller: FrontPageController = new FrontPageController
  when(clock.now()).thenCallRealMethod()

  val authHeaderWithAdminRole =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiYXpwIjoiMTIzIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoiZnJvbnRwYWdlOmFkbWluIiwiZ3R5IjoiY2xpZW50LWNyZWRlbnRpYWxzIiwicGVybWlzc2lvbnMiOlsiZnJvbnRwYWdlOmFkbWluIl19.A0qr0MgRH3O_jUODzN2Py13QL2R5FHdE3lZ2x-3ZTjA"

  val authHeaderWithWrongRole =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiYXpwIjoiMTIzIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoiZWFzdGVyOmVnZzpiZWZvcmU6STpxdWl0IiwiZ3R5IjoiY2xpZW50LWNyZWRlbnRpYWxzIiwicGVybWlzc2lvbnMiOlsiZWFzdGVyOmVnZzpiZWZvcmU6STpxdWl0Il19.Rl_qS-YtT698y40uEDSqpJV8zviQB30E4tBzapNCK6Q"

  val authHeaderWithoutAnyRoles =
    "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IlF6bEVPVFE1TTBOR01EazROakV4T0VKR01qYzJNalZGT0RoRVFrRTFOVUkyTmtFMFJUUXlSZyJ9.eyJpc3MiOiJodHRwczovL25kbGEtdGVzdC5ldS5hdXRoMC5jb20vIiwic3ViIjoiZnNleE9DZkpGR09LdXkxQzJlNzFPc3ZRd3EwTldLQUtAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNjgzODg0OTQ4LCJleHAiOjE2ODM4OTkzNDgsImF6cCI6ImZzZXhPQ2ZKRkdPS3V5MUMyZTcxT3N2UXdxME5XS0FLIiwic2NvcGUiOiIiLCJndHkiOiJjbGllbnQtY3JlZGVudGlhbHMiLCJwZXJtaXNzaW9ucyI6W119.oznL95qVdP7HFTwwVm8ZfVv1GSW3_mNKTbAt9No8-PI"

  val malformedNewFrontPage: String = """{"malformed": "x"}"""

  val sampleNewFrontPage: String =
    """{
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
    val request =
      quickRequest
        .post(uri"http://localhost:$serverPort/frontpage-api/v1/frontpage")
        .readTimeout(Duration.Inf)

    val response = simpleHttpClient.send(request)
    response.code.code should be(401)
  }

  test("That POST / returns 403 if auth header does not have expected role") {
    val request =
      quickRequest
        .post(uri"http://localhost:$serverPort/frontpage-api/v1/frontpage")
        .readTimeout(Duration.Inf)
        .headers(Map("Authorization" -> authHeaderWithWrongRole))

    val response = simpleHttpClient.send(request)
    response.code.code should be(403)
  }

  test("That POST / returns 403 if auth header does not have any roles") {
    val request =
      quickRequest
        .post(uri"http://localhost:$serverPort/frontpage-api/v1/frontpage")
        .readTimeout(Duration.Inf)
        .headers(Map("Authorization" -> authHeaderWithoutAnyRoles))

    val response = simpleHttpClient.send(request)
    response.code.code should be(403)
  }

  test("That POST / returns 200 if auth header does have correct role") {
    when(writeService.createFrontPage(any)).thenReturn(Success(FrontPageDTO(1, List())))

    val request =
      quickRequest
        .post(uri"http://localhost:$serverPort/frontpage-api/v1/frontpage")
        .readTimeout(Duration.Inf)
        .headers(Map("Authorization" -> authHeaderWithAdminRole))
        .body(sampleNewFrontPage)

    val response = simpleHttpClient.send(request)
    response.code.code should be(200)
  }

  test("That POST / returns 400 if auth header does have correct role but the json body is malformed") {
    when(writeService.createFrontPage(any)).thenReturn(Success(model.api.FrontPageDTO(1, List())))

    val request =
      quickRequest
        .post(uri"http://localhost:$serverPort/frontpage-api/v1/frontpage")
        .readTimeout(Duration.Inf)
        .headers(Map("Authorization" -> authHeaderWithAdminRole))
        .body(malformedNewFrontPage)

    val response = simpleHttpClient.send(request)
    response.code.code should be(400)
  }

  test("That GET / returns 200 when the frontpage is available") {
    val frontPage = model.api.FrontPageDTO(articleId = 1, menu = List.empty)

    when(readService.getFrontPage).thenReturn(Success(frontPage))
    val request = quickRequest.get(uri"http://localhost:$serverPort/frontpage-api/v1/frontpage")

    val response = simpleHttpClient.send(request)
    response.code.code should be(200)
  }

  test("That GET / returns 404 if frontpage is not found") {
    when(readService.getFrontPage).thenReturn(Failure(common.NotFoundException("Front page was not found")))
    val request = quickRequest.get(uri"http://localhost:$serverPort/frontpage-api/v1/frontpage")

    val response = simpleHttpClient.send(request)
    response.code.code should be(404)
  }
}
