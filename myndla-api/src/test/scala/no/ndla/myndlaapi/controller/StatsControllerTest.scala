/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.controller

import no.ndla.common.Clock
import no.ndla.common.model.api.SingleResourceStatsDTO
import no.ndla.common.model.domain.TryMaybe
import no.ndla.myndlaapi.model.api.{StatsDTO, UserStatsDTO}
import no.ndla.myndlaapi.TestEnvironment
import no.ndla.network.tapir.{ErrorHelpers, Routes, TapirController}
import no.ndla.scalatestsuite.UnitTestSuite
import no.ndla.tapirtesting.TapirControllerTest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import sttp.client3.quick.*

import scala.util.Success

class StatsControllerTest extends UnitTestSuite with TestEnvironment with TapirControllerTest {
  override implicit lazy val clock: Clock                           = mock[Clock]
  override implicit lazy val errorHelpers: ErrorHelpers             = new ErrorHelpers
  override implicit lazy val errorHandling: ControllerErrorHandling = new ControllerErrorHandling
  override implicit lazy val routes: Routes                         = new Routes
  val controller: StatsController                                   = new StatsController
  override implicit lazy val services: List[TapirController]        = List(controller)

  test("That getting stats returns in fact stats") {
    when(folderReadService.getStats).thenReturn(
      TryMaybe.from(StatsDTO(1, 2, 3, 4, 5, 6, 7, List.empty, Map.empty, UserStatsDTO(1, 2, 3, 4, 5, 6, 7)))
    )

    val response = simpleHttpClient.send(quickRequest.get(uri"http://localhost:$serverPort/myndla-api/v1/stats"))
    response.code.code should be(200)
  }

  test("That getting multiple resourceTypes for id works") {
    when(folderReadService.getFavouriteStatsForResource(any, any))
      .thenReturn(
        Success(
          List(SingleResourceStatsDTO("article", "123", 21), SingleResourceStatsDTO("multidisciplinary", "123", 1))
        )
      )
    val response = simpleHttpClient.send(
      quickRequest.get(uri"http://localhost:$serverPort/myndla-api/v1/stats/favorites/article,multidisciplinary/123")
    )
    response.code.code should be(200)

    verify(folderReadService, times(1)).getFavouriteStatsForResource(List("123"), List("article", "multidisciplinary"))
  }

}
