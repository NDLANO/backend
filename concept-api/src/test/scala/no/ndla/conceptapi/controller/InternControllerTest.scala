/*
 * Part of NDLA concept-api.
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */
package no.ndla.conceptapi.controller

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import no.ndla.conceptapi.{Eff, TestEnvironment, UnitSuite}
import no.ndla.network.tapir.Service
import org.json4s.DefaultFormats

class InternControllerTest extends UnitSuite with TestEnvironment {
  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
  val serverPort: Int                       = findFreePort
  val controller                            = new InternController
  override val services: List[Service[Eff]] = List(controller)

  override def beforeAll(): Unit = {
    IO { Routes.startJdkServer(this.getClass.getName, serverPort) {} }.unsafeRunAndForget()
    Thread.sleep(1000)
  }

  override def beforeEach(): Unit = {
    reset(clock)
    when(clock.now()).thenCallRealMethod()
  }

  test("That no endpoints are shadowed") {
    import sttp.tapir.testing.EndpointVerifier
    val errors = EndpointVerifier(controller.endpoints.map(_.endpoint))
    if (errors.nonEmpty) {
      val errString = errors.map(e => e.toString).mkString("\n\t- ", "\n\t- ", "")
      fail(s"Got errors when verifying ${controller.serviceName} controller:$errString")
    }
  }

}
