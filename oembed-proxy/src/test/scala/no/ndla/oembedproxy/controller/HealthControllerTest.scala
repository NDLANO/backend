/*
 * Part of NDLA oembed-proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.controller

import cats.effect.IO
import no.ndla.oembedproxy.{TestEnvironment, UnitSuite}
import org.http4s.jetty.server.JettyBuilder
import sttp.client3.quick._

class HealthControllerTest extends UnitSuite with TestEnvironment {

  val serverPort: Int = findFreePort

  lazy val controller = new HealthController
  controller.setWarmedUp()
  override def beforeAll(): Unit = {
    import cats.effect.unsafe.implicits.global
    val app         = Routes.build(List(controller))
    var serverReady = false

    JettyBuilder[IO]
      .mountHttpApp(app, "/")
      .bindHttp(serverPort)
      .resource
      .use(server => {
        IO {
          println(s"${this.getClass.toString} is running server on ${server.address}")
          serverReady = true
        }.flatMap(_ => IO.never)
      })
      .unsafeToFuture()

    blockUntil(() => serverReady)
  }

  test("That /health returns 200 ok") {
    val response = simpleHttpClient.send(quickRequest.get(uri"http://localhost:$serverPort/health"))
    response.code.code should be(200)
  }

}
