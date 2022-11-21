/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi
import cats.effect.{ExitCode, IO}
import com.comcast.ip4s.{Host, Port}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.Router
import scalaj.http.Http

class FilmPageControllerTest extends UnitSuite with TestEnvironment {

  val serverPort: Int = findFreePort

  override val filmPageController = new FilmPageController()

  override def beforeAll(): Unit = {
    val app = Router[IO](
      "/" -> NdlaMiddleware(List(filmPageController))
    ).orNotFound

    val serverBuilder = EmberServerBuilder
      .default[IO]
      .withHost(Host.fromString("0.0.0.0").get)
      .withPort(Port.fromInt(serverPort).get)
      .withHttpApp(app)
      .build
      .use(server => {
        IO {
          println(s"We running now boys ${server.address}")
        }.flatMap(_ => IO.never)
      })
    Thread.sleep(1000)
  }

  test("Should return 200 when frontpage exist") {
    when(readService.filmFrontPage(None)).thenReturn(Some(TestData.apiFilmFrontPage))
    val response = Http(s"http://localhost:$serverPort/frontpage-api/v1/film").method("GET").asString
    response.code should equal(200)
  }

  test("Should return 404 when no frontpage found") {
    when(readService.filmFrontPage(None)).thenReturn(None)
    val response = Http(s"http://localhost:$serverPort/frontpage-api/v1/film").method("GET").asString
    response.code should equal(404)
  }

}
