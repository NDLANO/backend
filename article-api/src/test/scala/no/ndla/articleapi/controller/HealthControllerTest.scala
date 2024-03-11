/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import no.ndla.articleapi.{Eff, TestEnvironment, UnitSuite}
import no.ndla.tapirtesting.TapirControllerTest
import sttp.client3.quick._

class HealthControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest[Eff] {
  override val controller: TapirHealthController[Eff] = new TapirHealthController[Eff]()
  controller.setWarmedUp()

  test("That /health returns 200 ok") {
    val response = simpleHttpClient.send(quickRequest.get(uri"http://localhost:$serverPort/health"))
    response.code.code should be(200)
  }

}
