/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */
package no.ndla.frontpageapi.controller

import cats.effect.IO
import cats.implicits._
import no.ndla.frontpageapi.model.api._
import no.ndla.frontpageapi.service.{ReadService, WriteService}
import sttp.tapir._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import no.ndla.frontpageapi.auth.UserInfo
import no.ndla.frontpageapi.model.domain.Errors.ValidationException
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint

import scala.util.{Failure, Success}

trait FilmPageController {
  this: ReadService with WriteService with ErrorHelpers with Service =>
  val filmPageController: FilmPageController

  class FilmPageController extends SwaggerService {
    override val prefix: EndpointInput[Unit] = "frontpage-api" / "v1" / "filmfrontpage"
    override val endpoints: List[ServerEndpoint[Any, IO]] = List(
      endpoint.get
        .summary("Get data to display on the film front page")
        .in(query[Option[String]]("language"))
        .out(jsonBody[FilmFrontPageData])
        .errorOut(oneOf(oneOfVariant(GenericError), oneOfVariant(NotFoundError)))
        .serverLogicPure { language =>
          readService.filmFrontPage(language) match {
            case Some(s) => s.asRight
            case None    => ErrorHelpers.notFound.asLeft
          }
        },
      endpoint.post
        .summary("Update film front page")
        .errorOut(
          oneOf(
            oneOfVariant(GenericError),
            oneOfVariant(UnprocessableEntityError),
            oneOfVariant(ForbiddenError),
            oneOfVariant(UnauthorizedError)
          )
        )
        .in(jsonBody[NewOrUpdatedFilmFrontPageData])
        .out(jsonBody[FilmFrontPageData])
        .securityIn(auth.bearer[Option[UserInfo]]())
        .serverSecurityLogicPure {
          case Some(user) if user.canWrite => user.asRight
          case Some(_)                     => ErrorHelpers.forbidden.asLeft
          case None                        => ErrorHelpers.unauthorized.asLeft
        }
        .serverLogicPure { _ => filmFrontPage =>
          writeService.updateFilmFrontPage(filmFrontPage) match {
            case Success(s) => s.asRight
            case Failure(ex: ValidationException) =>
              ErrorHelpers.unprocessableEntity(ex.getMessage).asLeft
            case Failure(_) => ErrorHelpers.generic.asLeft
          }
        }
    )
  }
}
