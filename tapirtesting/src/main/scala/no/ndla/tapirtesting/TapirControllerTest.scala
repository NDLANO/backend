/*
 * Part of NDLA tapirtesting
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.tapirtesting

import no.ndla.common.Clock
import no.ndla.common.configuration.HasBaseProps
import no.ndla.network.tapir.{NdlaMiddleware, Routes, Service, TapirErrorHelpers}
import no.ndla.scalatestsuite.UnitTestSuite

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

trait TapirControllerTest[F[_]]
    extends UnitTestSuite
    with Routes[F]
    with NdlaMiddleware
    with HasBaseProps
    with TapirErrorHelpers
    with Clock {
  val serverPort: Int = findFreePort
  val controller: Service[F]
  override def services: List[Service[F]] = List(controller)

  override def beforeAll(): Unit = {
    implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1))
    Future { Routes.startJdkServer(s"TapirControllerTest:${this.getClass.getName}", serverPort) {} }: Unit
    Thread.sleep(1000)
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
