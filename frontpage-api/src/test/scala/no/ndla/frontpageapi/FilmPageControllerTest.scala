/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import sttp.client3.quick._

class FilmPageControllerTest extends UnitSuite with TestEnvironment {

  val serverPort: Int = findFreePort

  override val filmPageController = new FilmPageController()
  override val services           = List(filmPageController)

  override def beforeAll(): Unit = {
    IO { Routes.startJdkServer(this.getClass.getName, serverPort) {} }.unsafeRunAndForget()
    Thread.sleep(1000)
  }

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
