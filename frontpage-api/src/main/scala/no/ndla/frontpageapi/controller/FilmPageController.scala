/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.controller
import cats.effect.{Effect, IO}
import io.circe.generic.auto._
import no.ndla.frontpageapi.auth.UserInfo
import no.ndla.frontpageapi.model.api._
import no.ndla.frontpageapi.model.domain.Errors.ValidationException
import no.ndla.frontpageapi.service.{ReadService, WriteService}
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.rho.swagger.{SecOps, SwaggerSyntax}

import scala.util.{Failure, Success}

trait FilmPageController {
  this: ReadService with WriteService with ErrorHelpers =>
  val filmPageController: FilmPageController[IO]

  class FilmPageController[F[+_]: Effect](swaggerSyntax: SwaggerSyntax[F]) extends AuthController[F] {

    import swaggerSyntax._

    "Get data to display on the film front page" **
      GET +? param[Option[String]]("language") |>> { language: Option[String] =>
        {
          readService.filmFrontPage(language) match {
            case Some(s) => Ok(s)
            case None    => NotFound(ErrorHelpers.notFound)
          }
        }
      }

    AuthOptions.^^("Update film front page" ** POST) >>> Auth.auth ^ NewOrUpdatedFilmFrontPageData.decoder |>> {
      (user: Option[UserInfo], filmFrontPage: NewOrUpdatedFilmFrontPageData) =>
        {
          val x = user match {
            case Some(user) if user.canWrite =>
              writeService.updateFilmFrontPage(filmFrontPage) match {
                case Success(s) => Ok(s)
                case Failure(ex: ValidationException) =>
                  UnprocessableEntity(ErrorHelpers.unprocessableEntity(ex.getMessage))
                case Failure(_) => InternalServerError(ErrorHelpers.generic)
              }
            case Some(_) => Forbidden(ErrorHelpers.forbidden)
            case None    => Unauthorized(ErrorHelpers.unauthorized)
          }
          x
        }
    }

  }
}
