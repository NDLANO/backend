/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi

import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import no.ndla.common.model.NDLADate
import no.ndla.network.tapir.TapirServer
import sttp.client3.quick._

class SubjectPageControllerTest extends UnitSuite with TestEnvironment {

  val serverPort: Int = findFreePort

  override val subjectPageController = new SubjectPageController()

  override def beforeAll(): Unit = {
    val app    = Routes.build(List(subjectPageController))
    val server = TapirServer(this.getClass.getName, serverPort, app, enableMelody = false)()
    server.runInBackground()
    blockUntil(() => server.isReady)
  }

  test("Should return 400 with cool custom message if bad request") {
    when(clock.now()).thenReturn(NDLADate.now())
    val response =
      simpleHttpClient.send(
        quickRequest.get(uri"http://localhost:$serverPort/frontpage-api/v1/subjectpage/1?fallback=noefeil")
      )
    response.code.code should equal(400)
    val expectedBody =
      ErrorHelpers.badRequest("Invalid value for: query parameter fallback").asJson.dropNullValues.noSpaces
    response.body should be(expectedBody)
  }

}
