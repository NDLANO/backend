/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import no.ndla.articleapi.{Eff, TestEnvironment, UnitSuite}
import no.ndla.network.tapir.Service
import sttp.client3.quick._

class HealthControllerTest extends UnitSuite with TestEnvironment {
  val serverPort: Int = findFreePort

  override val healthController = new TapirHealthController[Eff]()
  healthController.setWarmedUp()
  override val services: List[Service[Eff]] = List(healthController)

  override def beforeAll(): Unit = {
    IO { Routes.startJdkServer(this.getClass.getName, serverPort) {} }.unsafeRunAndForget()
    Thread.sleep(1000)
  }

  test("That /health returns 200 ok") {
    val response = simpleHttpClient.send(quickRequest.get(uri"http://localhost:$serverPort/health"))
    response.code.code should be(200)
  }

}
