/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.controller

import cats.effect.IO
import io.circe.generic.auto._
import no.ndla.frontpageapi.model.api._
import no.ndla.frontpageapi.service.{ReadService, WriteService}
import no.ndla.network.tapir.Service
import no.ndla.network.tapir.TapirErrors.errorOutputsFor
import no.ndla.network.tapir.auth.Permission.FRONTPAGE_API_ADMIN
import no.ndla.network.tapir.auth.TokenUser
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint

trait FrontPageController {
  this: ReadService with WriteService with ErrorHelpers with Service =>
  val frontPageController: FrontPageController

  class FrontPageController() extends SwaggerService {
    override val prefix: EndpointInput[Unit] = "frontpage-api" / "v1" / "frontpage"

    import ErrorHelpers._

    val getFrontPage: ServerEndpoint[Any, IO] = endpoint.get
      .summary("Get data to display on the front page")
      .out(jsonBody[FrontPage])
      .errorOut(errorOutputsFor(404))
      .serverLogic { _ =>
        readService.getFrontPage.handleErrorsOrOk
      }

    val newFrontPage: ServerEndpoint[Any, IO] = endpoint.post
      .summary("Create front page")
      .in(jsonBody[FrontPage])
      .errorOut(errorOutputsFor(400, 401, 403, 404))
      .out(jsonBody[FrontPage])
      .securityIn(auth.bearer[Option[TokenUser]]())
      .serverSecurityLogicPure(requireScope(FRONTPAGE_API_ADMIN))
      .serverLogic { _ => frontPage =>
        writeService
          .createFrontPage(frontPage)
          .handleErrorsOrOk
      }

    override val endpoints: List[ServerEndpoint[Any, IO]] = List(getFrontPage, newFrontPage)
  }
}
