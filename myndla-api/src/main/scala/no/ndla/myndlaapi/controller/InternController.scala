/*
 * Part of NDLA myndla-api.
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.controller

import no.ndla.myndlaapi.service.ImportService
import no.ndla.myndlaapi.{Eff, Props}
import no.ndla.network.tapir.Service
import no.ndla.network.tapir.TapirErrors.errorOutputsFor
import sttp.tapir._
import sttp.tapir.server.ServerEndpoint

trait InternController {
  this: Props with ErrorHelpers with ImportService =>
  val internController: InternController

  class InternController extends Service[Eff] {
    override val prefix: EndpointInput[Unit] = "intern"
    override val enableSwagger               = false

    def importNodebb: ServerEndpoint[Any, Eff] = endpoint.post
      .in("arena" / "import")
      .errorOut(errorOutputsFor())
      .out(emptyOutput)
      .serverLogicPure { _ =>
        importService.importArenaDataFromNodeBB().handleErrorsOrOk
      }

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      importNodebb
    )
  }
}
