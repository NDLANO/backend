/*
 * Part of NDLA oembed-proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.controller

import no.ndla.oembedproxy.{Eff, TestEnvironment, UnitSuite}
import no.ndla.tapirtesting.TapirControllerTest
import sttp.client3.quick._

class HealthControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest[Eff] {

  lazy val controller = new TapirHealthController[Eff]
  controller.setWarmedUp()

  test("That /health returns 200 ok") {
    val response = simpleHttpClient.send(quickRequest.get(uri"http://localhost:$serverPort/health"))
    response.code.code should be(200)
  }

}
