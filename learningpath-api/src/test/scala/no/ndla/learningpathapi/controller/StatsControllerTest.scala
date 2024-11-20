/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.controller

import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}
import no.ndla.tapirtesting.TapirControllerTest
import sttp.client3.quick.*

class StatsControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest {

  val controller: StatsController              = new StatsController
  override def services: List[TapirController] = List(controller)

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

}
