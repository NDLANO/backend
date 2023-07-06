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
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.Service
import no.ndla.network.tapir.TapirErrors.errorOutputsFor
import no.ndla.network.tapir.auth.Permission.FRONTPAGE_API_ADMIN
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.server.ServerEndpoint

trait FrontPageController {
  this: ReadService with WriteService with ErrorHelpers with Service =>
  val frontPageController: FrontPageController

  class FrontPageController() extends SwaggerService {
    override val prefix: EndpointInput[Unit] = "frontpage-api" / "v1" / "frontpage"

    import ErrorHelpers._

    val myHeader = sttp.tapir
      .header[Option[String]]("TestHeader")
      .description("yessir")
      .mapDecode(mbHeader => DecodeResult.Value(mbHeader.map(_.replaceFirst("Bearer ", ""))))(x => x)

    val getFrontPage: ServerEndpoint[Any, IO] = endpoint.get
      .summary("Get data to display on the front page")
      .out(jsonBody[FrontPage])
      .in(myHeader)
      .errorOut(errorOutputsFor(404))
      .serverLogic { header =>
        println(s"We got header: ${header}")
        readService.getFrontPage.handleErrorsOrOk
      }

    val newFrontPage: ServerEndpoint[Any, IO] = endpoint.post
      .summary("Create front page")
      .in(jsonBody[FrontPage])
      .errorOut(errorOutputsFor(400, 401, 403, 404))
      .out(jsonBody[FrontPage])
      .requirePermission(FRONTPAGE_API_ADMIN)
      .serverLogic { _ => frontPage =>
        writeService
          .createFrontPage(frontPage)
          .handleErrorsOrOk
      }

    override val endpoints: List[ServerEndpoint[Any, IO]] = List(getFrontPage, newFrontPage)
  }
}
