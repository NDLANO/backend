/*
 * Part of NDLA network
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir

import no.ndla.common.Clock
import no.ndla.common.configuration.BaseProps
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import no.ndla.scalatestsuite.UnitTestSuite
import no.ndla.tapirtesting.TapirControllerTest
import sttp.client3.quick.*
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint

class TapirUtilStatusFallbackTest extends UnitTestSuite with TapirControllerTest {
  override implicit lazy val props: BaseProps = new BaseProps {
    override def ApplicationPort: Int    = findFreePort
    override def ApplicationName: String = "TapirUtilStatusFallbackTest"
  }

  override implicit lazy val clock: Clock                 = new Clock
  override implicit lazy val errorHelpers: ErrorHelpers   = new ErrorHelpers
  override implicit lazy val errorHandling: ErrorHandling = new ErrorHandling() {
    override def handleErrors: PartialFunction[Throwable, AllErrors] = PartialFunction.empty
  }
  implicit val myNDLAApiClient: MyNDLAApiClient              = mock[MyNDLAApiClient]
  override val controller: TapirUtilStatusFallbackController = new TapirUtilStatusFallbackController
  override implicit lazy val services: List[TapirController] = List(controller)
  override implicit lazy val routes: Routes                  = new Routes

  class TapirUtilStatusFallbackController(using
      errorHelpers: ErrorHelpers,
      errorHandling: ErrorHandling,
      myNDLAApiClient: MyNDLAApiClient,
  ) extends TapirController {
    override val prefix: EndpointInput[Unit]               = "tapir-util"
    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(undocumentedStatusEndpoint)

    private def undocumentedStatusEndpoint: ServerEndpoint[Any, Eff] = endpoint
      .get
      .in("undocumented-status")
      .out(stringBody)
      .errorOut(errorOutputsFor(400))
      .serverLogicPure { _ =>
        Left(errorHelpers.conflict("Expected undocumented status code fallback"))
      }
  }

  test("errorOutputsFor should preserve status code for undocumented errors") {
    val response =
      simpleHttpClient.send(quickRequest.get(uri"http://localhost:$serverPort/tapir-util/undocumented-status"))

    response.code.code should be(409)
  }
}
