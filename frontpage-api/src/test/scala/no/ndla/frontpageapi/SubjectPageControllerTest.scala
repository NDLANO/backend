/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.comcast.ip4s.{Host, Port}
import io.circe.syntax.EncoderOps
import io.circe.generic.auto._
import org.http4s.ember.server.EmberServerBuilder
import sttp.client3.quick._

import java.time.LocalDateTime

class SubjectPageControllerTest extends UnitSuite with TestEnvironment {

  val serverPort: Int = findFreePort

  override val subjectPageController = new SubjectPageController()

  override def beforeAll(): Unit = {
    val app = Routes.build(List(subjectPageController))

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

  test("Should return 400 with cool custom message if bad request") {
    when(clock.now()).thenReturn(LocalDateTime.now())
    val response =
      simpleHttpClient.send(
        quickRequest.get(uri"http://localhost:$serverPort/frontpage-api/v1/subjectpage/1?fallback=noefeil")
      )
    response.code.code should equal(400)
    val expectedBody = ErrorHelpers.badRequest("Invalid value for: query parameter fallback").asJson.noSpaces
    response.body should be(expectedBody)
  }

}
