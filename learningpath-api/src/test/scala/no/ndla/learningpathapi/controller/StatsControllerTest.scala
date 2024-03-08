/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.controller

import no.ndla.learningpathapi.{Eff, TestEnvironment, UnitSuite}
import no.ndla.network.tapir.Service
import no.ndla.tapirtesting.TapirControllerTest
import org.json4s.DefaultFormats
import sttp.client3.quick._

class StatsControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest[Eff] {

  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
  val controller                            = new StatsController
  override def services: List[Service[Eff]] = List(controller)

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
