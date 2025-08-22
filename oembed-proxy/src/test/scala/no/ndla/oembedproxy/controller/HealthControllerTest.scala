/*
 * Part of NDLA oembed-proxy
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.controller

import no.ndla.network.tapir.{Routes, TapirErrorHandling, TapirHealthController}
import no.ndla.oembedproxy.{TestEnvironment, UnitSuite}
import no.ndla.tapirtesting.TapirControllerTest
import sttp.client3.quick.*

class HealthControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest {
  override implicit val routes: Routes                    = ???
  override implicit val errorHandling: TapirErrorHandling = ???
  val controller: TapirHealthController                   = new TapirHealthController
  controller.setWarmedUp()

  test("That /health returns 200 ok") {
    val response = simpleHttpClient.send(quickRequest.get(uri"http://localhost:$serverPort/health"))
    response.code.code should be(200)
  }

}
