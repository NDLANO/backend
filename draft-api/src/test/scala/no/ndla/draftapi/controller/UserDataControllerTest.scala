/*
 * Part of NDLA draft-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.controller

import no.ndla.draftapi.model.api.UpdatedUserData
import no.ndla.draftapi.{Eff, TestData, TestEnvironment, UnitSuite}
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.tapirtesting.TapirControllerTest
import org.json4s.DefaultFormats
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.postgresql.util.{PSQLException, PSQLState}
import sttp.client3.quick.*

import scala.util.{Failure, Success}

class UserDataControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest[Eff] {
  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
  val controller: UserDataController        = new UserDataController()

  override def beforeEach(): Unit = {
    reset(clock)
    reset(readService)
    when(clock.now()).thenCallRealMethod()
  }

  test("GET / should return 200 if user has access roles and the user exists in database") {
    when(readService.getUserData(any[String])).thenReturn(Success(TestData.emptyApiUserData))

    val res = simpleHttpClient.send(
      quickRequest
        .get(uri"http://localhost:$serverPort/draft-api/v1/user-data")
        .headers(Map("Authorization" -> TestData.authHeaderWithWriteRole))
    )
    res.code.code should be(200)
  }

  test("GET / should return 403 if user has no access roles") {
    val res = simpleHttpClient.send(
      quickRequest
        .get(uri"http://localhost:$serverPort/draft-api/v1/user-data")
        .headers(Map("Authorization" -> TestData.authHeaderWithoutAnyRoles))
    )
    res.code.code should be(403)
  }

  test("GET / should return 500 if there was error returning the data") {
    when(readService.getUserData(any[String])).thenReturn(Failure(new PSQLException("error", PSQLState.UNKNOWN_STATE)))

    val res = simpleHttpClient.send(
      quickRequest
        .get(uri"http://localhost:$serverPort/draft-api/v1/user-data")
        .headers(Map("Authorization" -> TestData.authHeaderWithWriteRole))
    )
    res.code.code should be(500)
  }

  test("PATCH / should return 200 if user has access roles and data has been updated correctly") {
    when(writeService.updateUserData(any[UpdatedUserData], any[TokenUser]))
      .thenReturn(Success(TestData.emptyApiUserData))

    val res = simpleHttpClient.send(
      quickRequest
        .patch(uri"http://localhost:$serverPort/draft-api/v1/user-data")
        .body("{}")
        .headers(Map("Authorization" -> TestData.authHeaderWithWriteRole))
    )
    res.code.code should be(200)
  }

  test("PATCH / should return 403 if user has no access roles") {
    val res = simpleHttpClient.send(
      quickRequest
        .patch(uri"http://localhost:$serverPort/draft-api/v1/user-data")
        .headers(Map("Authorization" -> TestData.authHeaderWithoutAnyRoles))
    )
    res.code.code should be(403)
  }
}
