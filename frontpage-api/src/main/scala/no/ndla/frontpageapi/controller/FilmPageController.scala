/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.controller
import cats.Monad
import cats.effect.{Effect, IO}
import io.circe.generic.auto._
import io.circe.syntax._
import no.ndla.frontpageapi.auth.UserInfo
import no.ndla.frontpageapi.model.api._
import no.ndla.frontpageapi.model.domain.Errors.ValidationException
import no.ndla.frontpageapi.service.{ReadService, WriteService}
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.rho.swagger.{SecOps, SwaggerSyntax}

import scala.util.{Failure, Success}

trait FilmPageController {
  this: ReadService with WriteService =>
  val filmPageController: FilmPageController[IO]

  class FilmPageController[F[+ _]: Effect](swaggerSyntax: SwaggerSyntax[F])(implicit F: Monad[F])
      extends AuthController[F] {

    import swaggerSyntax._

    "Get data to display on the film front page" **
      GET +? param[Option[String]]("language") |>> { language: Option[String] =>
      {
        readService.filmFrontPage(language) match {
          case Some(s) => Ok(s)
          case None    => NotFound(Error.notFound)
        }
      }
    }

    AuthOptions.^^("Update film front page" ** POST) >>> Auth.auth ^ NewOrUpdatedFilmFrontPageData.decoder |>> {
      (user: Option[UserInfo], filmFrontPage: NewOrUpdatedFilmFrontPageData) =>
        {
          val x = user match {
            case Some(user) if user.canWrite =>
              writeService.updateFilmFrontPage(filmFrontPage) match {
                case Success(s)                       => Ok(s)
                case Failure(ex: ValidationException) => UnprocessableEntity(Error.unprocessableEntity(ex.getMessage))
                case Failure(_)                       => InternalServerError(Error.generic)
              }
            case Some(_) => Forbidden(Error.forbidden)
            case None    => Unauthorized(Error.unauthorized)
          }
          x
        }
    }

  }
}
