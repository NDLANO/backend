/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi

import no.ndla.common.Clock
import no.ndla.frontpageapi.controller.{ControllerErrorHandling, FilmPageController}
import no.ndla.frontpageapi.service.{ReadService, WriteService}
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.tapir.{ErrorHandling, ErrorHelpers, Routes, TapirController}
import no.ndla.tapirtesting.TapirControllerTest
import org.mockito.Mockito.when
import sttp.client3.quick.*

class FilmPageControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest {
  override implicit lazy val clock: Clock                 = mock[Clock]
  override implicit lazy val errorHelpers: ErrorHelpers   = new ErrorHelpers
  override implicit lazy val errorHandling: ErrorHandling = new ControllerErrorHandling
  override implicit lazy val routes: Routes               = new Routes
  override val controller: TapirController                = {
    given ReadService     = readService
    given WriteService    = writeService
    given MyNDLAApiClient = myndlaApiClient
    given ErrorHelpers    = errorHelpers
    new FilmPageController
  }
  override implicit lazy val services: List[TapirController] = List(controller)

  test("Should return 200 when frontpage exist") {
    when(readService.filmFrontPage(None)).thenReturn(Some(TestData.apiFilmFrontPage))
    val response =
      simpleHttpClient.send(quickRequest.get(uri"http://localhost:$serverPort/frontpage-api/v1/filmfrontpage"))
    response.code.code should equal(200)
  }

  test("Should return 404 when no frontpage found") {
    when(clock.now()).thenCallRealMethod()
    when(readService.filmFrontPage(None)).thenReturn(None)
    val response =
      simpleHttpClient.send(quickRequest.get(uri"http://localhost:$serverPort/frontpage-api/v1/filmfrontpage"))
    response.code.code should equal(404)
  }

}
