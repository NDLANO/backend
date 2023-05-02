/*
 * Part of NDLA oembed-proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.controller

import no.ndla.network.tapir.TapirServer
import no.ndla.oembedproxy.{TestEnvironment, UnitSuite}
import sttp.client3.quick._

class HealthControllerTest extends UnitSuite with TestEnvironment {

  val serverPort: Int = findFreePort

  lazy val controller = new HealthController
  controller.setWarmedUp()
  override def beforeAll(): Unit = {
    val app    = Routes.build(List(controller))
    val server = TapirServer(this.getClass.getName, serverPort, app, enableMelody = false)()
    server.toFuture
    blockUntil(() => server.isReady)
  }

  test("That /health returns 200 ok") {
    val response = simpleHttpClient.send(quickRequest.get(uri"http://localhost:$serverPort/health"))
    response.code.code should be(200)
  }

}
