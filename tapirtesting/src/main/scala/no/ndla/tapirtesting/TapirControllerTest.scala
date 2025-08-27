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
import no.ndla.common.configuration.BaseProps
import no.ndla.network.tapir.{AllErrors, ErrorHandling, ErrorHelpers, Routes, TapirController}
import no.ndla.scalatestsuite.UnitTestSuite

import scala.compiletime.uninitialized

trait TapirControllerTest extends UnitTestSuite {
  val controller: TapirController
  val serverPort: Int = findFreePort

  implicit lazy val props: BaseProps
  implicit lazy val clock: Clock                 = new Clock
  implicit lazy val errorHelpers: ErrorHelpers   = new ErrorHelpers
  implicit lazy val errorHandling: ErrorHandling = new ErrorHandling() {
    override def handleErrors: PartialFunction[Throwable, AllErrors] = { case t: Throwable =>
      fail("Error handler not implemented in test")
    }
  }
  implicit lazy val services: List[TapirController] = List(controller)
  implicit lazy val routes: Routes                  = new Routes

  var server: HttpServer = uninitialized

  override def beforeAll(): Unit = {
    super.beforeAll()
    server = routes.startJdkServerAsync(s"TapirControllerTest:${this.getClass.getName}", serverPort) {}
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
