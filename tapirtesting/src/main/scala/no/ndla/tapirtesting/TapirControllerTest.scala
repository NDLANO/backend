/*
 * Part of NDLA tapirtesting
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.tapirtesting

import com.sun.net.httpserver.HttpServer
import no.ndla.common.Clock
import no.ndla.common.configuration.HasBaseProps
import no.ndla.network.NdlaClient
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.tapir.{Routes, TapirController, TapirErrorHandling}
import no.ndla.scalatestsuite.UnitTestSuite
import scala.compiletime.uninitialized

trait TapirControllerTest
    extends UnitTestSuite
    with Routes
    with TapirController
    with MyNDLAApiClient
    with NdlaClient
    with HasBaseProps
    with TapirErrorHandling
    with Clock {
  val serverPort: Int = findFreePort
  val controller: TapirController
  override def services: List[TapirController] = List(controller)

  var server: HttpServer = uninitialized

  override def beforeAll(): Unit = {
    super.beforeAll()
    server = Routes.startJdkServerAsync(s"TapirControllerTest:${this.getClass.getName}", serverPort) {}
    Thread.sleep(1000)
  }

  override def afterAll(): Unit = server.stop(0)

  test("That no endpoints are shadowed") {
    import sttp.tapir.testing.EndpointVerifier
    val errors = EndpointVerifier(controller.endpoints.map(_.endpoint))
    if (errors.nonEmpty) {
      val errString = errors.map(e => e.toString).mkString("\n\t- ", "\n\t- ", "")
      fail(s"Got errors when verifying ${controller.serviceName} controller:$errString")
    }
  }

}
