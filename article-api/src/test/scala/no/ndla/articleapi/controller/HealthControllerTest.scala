/*
 * Part of NDLA article-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import no.ndla.common.Clock
import no.ndla.network.tapir.{ErrorHandling, ErrorHelpers, Routes, TapirController, TapirHealthController}
import no.ndla.tapirtesting.TapirControllerTest
import sttp.client3.quick.*

class HealthControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest {
  override implicit lazy val clock: Clock                    = mock[Clock]
  override implicit lazy val errorHelpers: ErrorHelpers      = new ErrorHelpers
  override implicit lazy val errorHandling: ErrorHandling    = new ControllerErrorHandling
  override val controller: TapirHealthController             = new TapirHealthController()
  override implicit lazy val services: List[TapirController] = List(controller)
  override implicit lazy val routes: Routes                  = new Routes
  controller.setWarmedUp()

  test("That /health returns 200 ok") {
    val response = simpleHttpClient.send(quickRequest.get(uri"http://localhost:$serverPort/health"))
    response.code.code should be(200)
  }

}
