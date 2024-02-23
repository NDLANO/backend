/*
 * Part of NDLA search-api.
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.controller

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import no.ndla.searchapi.{TestEnvironment, UnitSuite}
import sttp.tapir.testing.EndpointVerifier

class InternControllerTest extends UnitSuite with TestEnvironment {
  val serverPort: Int                           = findFreePort
  override val converterService                 = new ConverterService
  val controller                                = new InternController
  override val services: List[InternController] = List(controller)

  override def beforeAll(): Unit = {
    IO { Routes.startJdkServer("InternControllerTest", serverPort) {} }.unsafeRunAndForget()
    Thread.sleep(1000)
  }

  test("That no endpoints are shadowed") {
    val errors = EndpointVerifier(controller.endpoints.map(_.endpoint))
    if (errors.nonEmpty) {
      val errString = errors.map(e => e.toString).mkString("\n\t- ", "\n\t- ", "")
      fail(s"Got errors when verifying ${controller.serviceName} controller:$errString")
    }
  }
}
