/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.controller

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import no.ndla.myndla.model.api.Stats
import no.ndla.myndlaapi.{Eff, TestEnvironment}
import no.ndla.network.tapir.Service
import no.ndla.scalatestsuite.UnitTestSuite
import sttp.client3.quick._

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

}
