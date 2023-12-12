/*
 * Part of NDLA oembed-proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.controller

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import no.ndla.oembedproxy.{Eff, TestEnvironment, UnitSuite}
import sttp.client3.quick._

class HealthControllerTest extends UnitSuite with TestEnvironment {

  val serverPort: Int = findFreePort

  lazy val controller   = new TapirHealthController[Eff]
  override val services = List(controller)
  controller.setWarmedUp()
  override def beforeAll(): Unit = {
    IO { Routes.startJdkServer(this.getClass.getName, serverPort) {} }.unsafeRunAndForget()
    Thread.sleep(1000)
  }

  test("That /health returns 200 ok") {
    val response = simpleHttpClient.send(quickRequest.get(uri"http://localhost:$serverPort/health"))
    response.code.code should be(200)
  }

}
