/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.controller

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import no.ndla.myndla.model.api.{SingleResourceStats, Stats}
import no.ndla.myndlaapi.{Eff, TestEnvironment}
import no.ndla.network.tapir.Service
import no.ndla.scalatestsuite.UnitTestSuite
import sttp.client3.quick._

import scala.util.Success

class StatsControllerTest extends UnitTestSuite with TestEnvironment {
  val serverPort: Int = findFreePort

  val controller                            = new StatsController()
  override val services: List[Service[Eff]] = List(controller)

  override def beforeAll(): Unit = {
    IO { Routes.startJdkServer(this.getClass.getName, serverPort) {} }.unsafeRunAndForget()
    Thread.sleep(1000)
  }

  test("That getting stats returns in fact stats") {
    when(folderReadService.getStats).thenReturn(Some(Stats(1, 2, 3, 4, 5, 6, List.empty)))

    val response = simpleHttpClient.send(quickRequest.get(uri"http://localhost:$serverPort/myndla-api/v1/stats"))
    response.code.code should be(200)
  }

  test("That getting multiple resourceTypes for id works") {
    when(folderReadService.getFavouriteStatsForResource(any, any)).thenReturn(Success(SingleResourceStats(1)))
    val response = simpleHttpClient.send(
      quickRequest.get(uri"http://localhost:$serverPort/myndla-api/v1/stats/favorites/article,multidisciplinary/123")
    )
    response.code.code should be(200)

    verify(folderReadService, times(1)).getFavouriteStatsForResource("123", List("article", "multidisciplinary"))
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
