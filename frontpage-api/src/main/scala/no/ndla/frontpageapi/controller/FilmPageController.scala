/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */
package no.ndla.frontpageapi.controller

import cats.implicits._
import io.circe.generic.auto._
import no.ndla.frontpageapi.Eff
import no.ndla.frontpageapi.model.api._
import no.ndla.frontpageapi.model.domain.Errors.ValidationException
import no.ndla.frontpageapi.service.{ReadService, WriteService}
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.Service
import no.ndla.network.tapir.TapirErrors.errorOutputsFor
import no.ndla.network.tapir.auth.Permission.FRONTPAGE_API_WRITE
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.server.ServerEndpoint

trait FilmPageController {
  this: ReadService with WriteService with ErrorHelpers =>
  val filmPageController: FilmPageController

  class FilmPageController extends Service[Eff] {
    import ErrorHelpers._

    override val serviceName: String         = "filmfrontpage"
    override val prefix: EndpointInput[Unit] = "frontpage-api" / "v1" / serviceName
    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      endpoint.get
        .summary("Get data to display on the film front page")
        .in(query[Option[String]]("language"))
        .out(jsonBody[FilmFrontPageData])
        .errorOut(errorOutputsFor(404))
        .serverLogicPure { language =>
          readService.filmFrontPage(language) match {
            case Some(s) => s.asRight
            case None    => ErrorHelpers.notFound.asLeft
          }
        },
      endpoint.post
        .summary("Update film front page")
        .errorOut(errorOutputsFor(400, 401, 403, 404, 422))
        .in(jsonBody[NewOrUpdatedFilmFrontPageData])
        .out(jsonBody[FilmFrontPageData])
        .requirePermission(FRONTPAGE_API_WRITE)
        .serverLogicPure { _ => filmFrontPage =>
          writeService.updateFilmFrontPage(filmFrontPage).partialOverride { case ex: ValidationException =>
            ErrorHelpers.unprocessableEntity(ex.getMessage)
          }
        }
    )
  }
}
