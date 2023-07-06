/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import no.ndla.network.tapir.TapirServer
import sttp.client3.quick._

class HealthControllerTest extends UnitSuite with TestEnvironment {
  val serverPort: Int = findFreePort

  override val healthController = new TapirHealthController()
  healthController.setWarmedUp()

  override def beforeAll(): Unit = {
    val app    = Routes.build(List(healthController))
    val server = TapirServer(this.getClass.getName, serverPort, app, enableMelody = false)()
    server.runInBackground()
    blockUntil(() => server.isReady)
  }

  test("That /health returns 200 ok") {
    val response = simpleHttpClient.send(quickRequest.get(uri"http://localhost:$serverPort/health"))
    response.code.code should be(200)
  }

}
