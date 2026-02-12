/*
 * Part of NDLA oembed-proxy
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.controller

import no.ndla.common.Clock as NDLAClock
import no.ndla.network.tapir.{ErrorHandling, ErrorHelpers, Routes, TapirController, TapirHealthController}
import no.ndla.oembedproxy.{TestEnvironment, UnitSuite}
import no.ndla.tapirtesting.TapirControllerTest
import sttp.client3.quick.*

class HealthControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest {
  val controller: TapirHealthController                      = new TapirHealthController
  override implicit lazy val services: List[TapirController] = List(controller)
  override implicit lazy val errorHandling: ErrorHandling    = new ControllerErrorHandling
  override implicit lazy val routes: Routes                  = new Routes
  override implicit lazy val clock: NDLAClock                = mock[NDLAClock]
  override implicit lazy val errorHelpers: ErrorHelpers      = new ErrorHelpers
  controller.setRunning()

  test("That /health returns 200 ok") {
    val response = simpleHttpClient.send(quickRequest.get(uri"http://localhost:$serverPort/health"))
    response.code.code should be(200)
  }

}
