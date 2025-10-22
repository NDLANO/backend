/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.controller

import no.ndla.common.Clock
import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}
import no.ndla.network.tapir.{ErrorHandling, ErrorHelpers, Routes, TapirController}
import no.ndla.tapirtesting.TapirControllerTest
import sttp.client3.quick.*

class StatsControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest {
  override implicit lazy val clock: Clock                    = mock[Clock]
  override implicit lazy val errorHelpers: ErrorHelpers      = new ErrorHelpers
  override implicit lazy val errorHandling: ErrorHandling    = new ControllerErrorHandling
  override implicit lazy val routes: Routes                  = new Routes
  val controller: StatsController                            = new StatsController
  override implicit lazy val services: List[TapirController] = List(controller)

  override def beforeEach(): Unit = {
    resetMocks()
  }

  test("That getting stats redirects to the correct endpoint") {
    val res = simpleHttpClient.send(
      quickRequest.get(uri"http://localhost:$serverPort/learningpath-api/v1/stats").followRedirects(false)
    )
    res.header("Location") should be(Some("/myndla-api/v1/stats"))
    res.code.code should be(301)
  }

}
