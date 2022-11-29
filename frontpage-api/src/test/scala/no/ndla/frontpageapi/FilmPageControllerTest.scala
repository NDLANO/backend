/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.comcast.ip4s.{Host, Port}
import org.http4s.ember.server.EmberServerBuilder
import scalaj.http.Http

class FilmPageControllerTest extends UnitSuite with TestEnvironment {

  val serverPort: Int = findFreePort

  override val filmPageController = new FilmPageController()

  override def beforeAll(): Unit = {
    val app = Routes.build(List(filmPageController))

    var serverReady = false

    EmberServerBuilder
      .default[IO]
      .withHost(Host.fromString("0.0.0.0").get)
      .withPort(Port.fromInt(serverPort).get)
      .withHttpApp(app)
      .build
      .use(server => {
        IO {
          println(s"${this.getClass.toString} is running server on ${server.address}")
          serverReady = true
        }.flatMap(_ => IO.never)
      })
      .unsafeToFuture()
    blockUntil(() => serverReady)
  }

  test("Should return 200 when frontpage exist") {
    when(readService.filmFrontPage(None)).thenReturn(Some(TestData.apiFilmFrontPage))
    val response = Http(s"http://localhost:$serverPort/frontpage-api/v1/filmfrontpage").method("GET").asString
    response.code should equal(200)
  }

  test("Should return 404 when no frontpage found") {
    when(readService.filmFrontPage(None)).thenReturn(None)
    val response = Http(s"http://localhost:$serverPort/frontpage-api/v1/filmfrontpage").method("GET").asString
    response.code should equal(404)
  }

}
