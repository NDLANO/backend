/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.controller

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import no.ndla.learningpathapi.{Eff, TestEnvironment, UnitSuite}
import no.ndla.network.tapir.Service
import org.json4s.DefaultFormats
import sttp.client3.quick._

class StatsControllerTest extends UnitSuite with TestEnvironment {

  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
  val serverPort: Int                       = findFreePort
  val controller                            = new StatsController
  override val services: List[Service[Eff]] = List(controller)

  override def beforeAll(): Unit = {
    IO { Routes.startJdkServer("StatsControllerTest", serverPort) {} }.unsafeRunAndForget()
    Thread.sleep(1000)
  }

  override def beforeEach(): Unit = {
    resetMocks()
  }

  test("That getting stats redirects to the correct endpoint") {
    val res = simpleHttpClient.send(
      quickRequest
        .get(uri"http://localhost:$serverPort/learningpath-api/v1/stats")
        .followRedirects(false)
    )
    res.header("Location") should be(Some("/myndla-api/v1/stats"))
    res.code.code should be(301)
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
