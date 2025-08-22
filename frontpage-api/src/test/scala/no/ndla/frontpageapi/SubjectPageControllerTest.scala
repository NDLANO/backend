/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi

import io.circe.syntax.EncoderOps
import no.ndla.common.Clock
import no.ndla.common.model.NDLADate
import no.ndla.frontpageapi.controller.SubjectPageController
import no.ndla.frontpageapi.service.{ReadService, WriteService}
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.tapir.{ErrorHelpers, Routes, TapirController}
import no.ndla.tapirtesting.TapirControllerTest
import org.mockito.Mockito.when
import sttp.client3.quick.*

class SubjectPageControllerTest extends UnitSuite with TestEnvironment with TapirControllerTest {
  override implicit lazy val clock = mock[Clock]
  override lazy val errorHelpers = new ErrorHelpers
  override lazy val routes = new Routes(using props, errorHelpers, services)
  override val controller: TapirController = {
    given ReadService = readService
    given WriteService = writeService
    given FrontpageApiProperties = props
    given Clock = clock
    given MyNDLAApiClient = myndlaApiClient
    given ErrorHelpers = errorHelpers
    new SubjectPageController
  }
  override implicit lazy val services: List[TapirController] = List(controller)

  test("Should return 400 with cool custom message if bad request") {
    when(clock.now()).thenReturn(NDLADate.now())
    val response =
      simpleHttpClient.send(
        quickRequest.get(uri"http://localhost:$serverPort/frontpage-api/v1/subjectpage/1?fallback=noefeil")
      )
    response.code.code should equal(400)
    val expectedBody =
      errorHelpers.badRequest("Invalid value for: query parameter fallback").asJson.dropNullValues.noSpaces
    response.body should be(expectedBody)
  }

}
