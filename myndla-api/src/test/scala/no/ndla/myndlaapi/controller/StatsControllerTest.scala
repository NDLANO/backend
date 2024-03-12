/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.controller

import no.ndla.myndla.model.api.{SingleResourceStats, Stats}
import no.ndla.myndlaapi.{Eff, TestEnvironment}
import no.ndla.scalatestsuite.UnitTestSuite
import no.ndla.tapirtesting.TapirControllerTest
import sttp.client3.quick._

import scala.util.Success

class StatsControllerTest extends UnitTestSuite with TestEnvironment with TapirControllerTest[Eff] {
  val controller = new StatsController()

  test("That getting stats returns in fact stats") {
    when(folderReadService.getStats).thenReturn(Some(Stats(1, 2, 3, 4, 5, 6, List.empty)))

    val response = simpleHttpClient.send(quickRequest.get(uri"http://localhost:$serverPort/myndla-api/v1/stats"))
    response.code.code should be(200)
  }

  test("That getting multiple resourceTypes for id works") {
    when(folderReadService.getFavouriteStatsForResource(any, any))
      .thenReturn(Success(List(SingleResourceStats("1", 21))))
    val response = simpleHttpClient.send(
      quickRequest.get(uri"http://localhost:$serverPort/myndla-api/v1/stats/favorites/article,multidisciplinary/123")
    )
    response.code.code should be(200)

    verify(folderReadService, times(1)).getFavouriteStatsForResource(List("123"), List("article", "multidisciplinary"))
  }

}
