/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi

import no.ndla.tapirtesting.TapirControllerTest
import org.mockito.Mockito.when
import sttp.client3.quick.*

class FilmPageControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest[Eff] {
  override val controller: FilmPageController = new FilmPageController()

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
