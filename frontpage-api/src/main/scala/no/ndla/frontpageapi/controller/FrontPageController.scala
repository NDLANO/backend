/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.controller

import no.ndla.common.model.api.FrontPageDTO
import no.ndla.frontpageapi.service.{ReadService, WriteService}
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.tapir.{ErrorHandling, ErrorHelpers, TapirController}
import no.ndla.network.tapir.NoNullJsonPrinter.*
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import no.ndla.network.tapir.auth.Permission.FRONTPAGE_API_ADMIN
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint

class FrontPageController(using
    readService: ReadService,
    writeService: WriteService,
    myNDLAApiClient: MyNDLAApiClient,
    errorHelpers: ErrorHelpers,
    errorHandling: ErrorHandling
) extends TapirController {
  override val serviceName: String         = "frontpage"
  override val prefix: EndpointInput[Unit] = "frontpage-api" / "v1" / serviceName

  def getFrontPage: ServerEndpoint[Any, Eff] = endpoint.get
    .summary("Get data to display on the front page")
    .out(jsonBody[FrontPageDTO])
    .errorOut(errorOutputsFor(404))
    .serverLogicPure { _ =>
      readService.getFrontPage
    }

  def newFrontPage: ServerEndpoint[Any, Eff] = endpoint.post
    .summary("Create front page")
    .in(jsonBody[FrontPageDTO])
    .errorOut(errorOutputsFor(400, 401, 403, 404))
    .out(jsonBody[FrontPageDTO])
    .requirePermission(FRONTPAGE_API_ADMIN)
    .serverLogicPure { _ => frontPage =>
      writeService
        .createFrontPage(frontPage)

    }

  override val endpoints: List[ServerEndpoint[Any, Eff]] = List(getFrontPage, newFrontPage)
}
