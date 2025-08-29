/*
 * Part of NDLA draft-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.controller

import no.ndla.common.Clock
import no.ndla.draftapi.model.api.UpdatedUserDataDTO
import no.ndla.draftapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.network.tapir.{ErrorHandling, ErrorHelpers, Routes, TapirController}
import no.ndla.tapirtesting.TapirControllerTest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.postgresql.util.{PSQLException, PSQLState}
import sttp.client3.quick.*

import scala.util.{Failure, Success}

class UserDataControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest {
  override implicit lazy val clock: Clock                    = mock[Clock]
  override implicit lazy val errorHelpers: ErrorHelpers      = new ErrorHelpers
  override implicit lazy val errorHandling: ErrorHandling    = new ControllerErrorHandling
  override val controller: UserDataController                = new UserDataController()
  override implicit lazy val services: List[TapirController] = List(controller)
  override implicit lazy val routes: Routes                  = new Routes

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
    when(writeService.updateUserData(any[UpdatedUserDataDTO], any[TokenUser]))
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
