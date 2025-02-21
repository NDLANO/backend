/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.controller

import no.ndla.myndlaapi.service.ImportService
import no.ndla.myndlaapi.Props
import no.ndla.network.tapir.TapirController
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint

trait InternController {
  this: Props & ErrorHandling & ImportService & TapirController =>
  val internController: InternController

  class InternController extends TapirController {
    override val prefix: EndpointInput[Unit] = "intern"
    override val enableSwagger               = false

    def importNodebb: ServerEndpoint[Any, Eff] = endpoint.post
      .in("arena" / "import")
      .errorOut(errorOutputsFor())
      .out(emptyOutput)
      .serverLogicPure { _ =>
        importService.importArenaDataFromNodeBB()
      }

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      importNodebb
    )
  }
}
