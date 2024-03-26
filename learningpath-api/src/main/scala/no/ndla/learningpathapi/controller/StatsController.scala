/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.controller

import no.ndla.learningpathapi.{Eff, Props}
import no.ndla.learningpathapi.model.api.ErrorHelpers
import no.ndla.learningpathapi.service.ReadService
import no.ndla.network.tapir.Service
import sttp.model.StatusCode
import sttp.tapir.EndpointInput
import sttp.tapir._
import sttp.tapir.server.ServerEndpoint

trait StatsController {
  this: ReadService with Props with ErrorHelpers =>
  val statsController: StatsController

  class StatsController extends Service[Eff] {
    override val serviceName: String         = "stats"
    override val prefix: EndpointInput[Unit] = "learningpath-api" / "v1" / serviceName
    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      getStats
    )

    def getStats: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Get stats for my-ndla usage.")
      .description("Get stats for my-ndla usage.")
      .deprecated()
      .errorOut(statusCode(StatusCode.MovedPermanently).and(header("Location", "/myndla-api/v1/stats")))
      .serverLogicPure(_ => Left(()))
  }
}
