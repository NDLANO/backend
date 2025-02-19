/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.controller

import no.ndla.learningpathapi.Props
import no.ndla.learningpathapi.model.api.ErrorHandling
import no.ndla.learningpathapi.service.ReadService
import no.ndla.network.tapir.TapirController
import sttp.model.StatusCode
import sttp.tapir.EndpointInput
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint

trait StatsController {
  this: ReadService & Props & ErrorHandling & TapirController =>
  val statsController: StatsController

  class StatsController extends TapirController {
    override val serviceName: String         = "stats"
    override val prefix: EndpointInput[Unit] = "learningpath-api" / "v1" / serviceName
    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      getStats
    )

    private def getStats: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Get stats for my-ndla usage.")
      .description("Get stats for my-ndla usage.")
      .deprecated()
      .errorOut(statusCode(StatusCode.MovedPermanently).and(header("Location", "/myndla-api/v1/stats")))
      .serverLogicPure(_ => Left(()))
  }
}
