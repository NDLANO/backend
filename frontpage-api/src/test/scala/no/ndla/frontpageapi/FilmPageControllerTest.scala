/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi
import cats.effect.{ContextShift, IO, Timer}
import no.ndla.frontpageapi.controller.NdlaMiddleware
import org.http4s.implicits._
import org.http4s.rho.swagger.syntax.{io => ioSwagger}
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import scalaj.http.Http

import java.io.IOException
import java.net.ServerSocket
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class FilmPageControllerTest extends UnitSuite with TestEnvironment {

  val serverPort: Int               = findFreePort
  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO]     = IO.timer(global)

  override val filmPageController = new FilmPageController[IO](ioSwagger)

  override def beforeAll(): Unit = {
    val app = Router[IO](
      "/filmfrontpage" -> NdlaMiddleware(filmPageController.toRoutes())
    ).orNotFound

    val serverBuilder = BlazeServerBuilder[IO](ExecutionContext.global).withHttpApp(app).bindLocal(serverPort)
    serverBuilder.resource.use(_ => IO.never).start.unsafeRunSync()
    Thread.sleep(100) // The server takes some time to actually listen
  }

  test("Should return 200 when frontpage exist") {
    when(readService.filmFrontPage(None)).thenReturn(Some(TestData.apiFilmFrontPage))
    val response = Http(s"http://localhost:$serverPort/filmfrontpage").method("GET").asString
    response.code should equal(200)
  }

  test("Should return 404 when no frontpage found") {
    when(readService.filmFrontPage(None)).thenReturn(None)
    val response = Http(s"http://localhost:$serverPort/filmfrontpage").method("GET").asString
    response.code should equal(404)
  }

}
