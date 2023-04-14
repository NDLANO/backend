/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.controller

import cats.effect.IO
import cats.implicits._
import no.ndla.frontpageapi.model.api._
import no.ndla.frontpageapi.service.{ReadService, WriteService}
import no.ndla.network.tapir.Service
import no.ndla.network.tapir.TapirErrors.errorOutputs
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint

trait FrontPageController {
  this: ReadService with WriteService with ErrorHelpers with Service =>
  val frontPageController: FrontPageController

  class FrontPageController() extends SwaggerService {
    override val prefix: EndpointInput[Unit] = "frontpage-api" / "v1" / "frontpage"

    override val endpoints: List[ServerEndpoint[Any, IO]] = List(
      endpoint.get
        .summary("Get data to display on the front page")
        .out(jsonBody[FrontPageData])
        .errorOut(errorOutputs)
        .serverLogicPure(_ =>
          readService.frontPage match {
            case Some(s) => s.asRight
            case None    => ErrorHelpers.notFound.asLeft
          }
        )
    )
  }
}
