/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.controller

import cats.Monad
import cats.effect.{Effect, IO}
import no.ndla.frontpageapi.FrontpageApiProperties
import no.ndla.frontpageapi.auth.UserInfo
import no.ndla.frontpageapi.model.api.{Error, NewSubjectFrontPageData, UpdatedSubjectFrontPageData}
import no.ndla.frontpageapi.model.domain.Errors.{NotFoundException, ValidationException}
import no.ndla.frontpageapi.service.{ReadService, WriteService}
import org.http4s.rho.swagger.{SecOps, SwaggerSyntax}

import scala.util.{Failure, Success}

trait SubjectPageController {
  this: ReadService with WriteService =>
  val subjectPageController: SubjectPageController[IO]

  class SubjectPageController[F[+ _]: Effect](swaggerSyntax: SwaggerSyntax[F])(implicit F: Monad[F])
      extends AuthController[F] {

    import swaggerSyntax._

    "Get data to display on a subject page" **
      GET / pathVar[Long]("subjectpage-id", "The subjectpage id") +? param[String](
      "language",
      FrontpageApiProperties.DefaultLanguage) & param[Boolean]("fallback", false) |>> {
      (id: Long, language: String, fallback: Boolean) =>
        {
          readService.subjectPage(id, language, fallback) match {
            case Some(s) => Ok(s)
            case None    => NotFound(Error.notFound)
          }
        }
    }

    AuthOptions.^^("Create new subject page" ** POST) >>> Auth.auth ^ NewSubjectFrontPageData.decoder |>> {
      (user: Option[UserInfo], newSubjectFrontPageData: NewSubjectFrontPageData) =>
        {
          user match {
            case Some(user) if user.canWrite =>
              writeService.newSubjectPage(newSubjectFrontPageData) match {
                case Success(s)                       => Ok(s)
                case Failure(ex: ValidationException) => UnprocessableEntity(Error.unprocessableEntity(ex.getMessage))
                case Failure(_)                       => InternalServerError(Error.generic)
              }
            case Some(_) => Forbidden(Error.forbidden)
            case None    => Unauthorized(Error.unauthorized)
          }
        }
    }

    AuthOptions.^^("Update subject page" **
      PATCH) / pathVar[Long]("subjectpage-id", "The subjectpage id") +? param[String](
      "language",
      FrontpageApiProperties.DefaultLanguage) >>> Auth.auth ^ UpdatedSubjectFrontPageData.decoder |>> {
      (id: Long, language: String, user: Option[UserInfo], subjectPage: UpdatedSubjectFrontPageData) =>
        {
          user match {
            case Some(user) if user.canWrite =>
              writeService.updateSubjectPage(id, subjectPage, language) match {
                case Success(s)                       => Ok(s)
                case Failure(_: NotFoundException)    => NotFound(Error.notFound)
                case Failure(ex: ValidationException) => UnprocessableEntity(Error.unprocessableEntity(ex.getMessage))
                case Failure(_)                       => InternalServerError(Error.generic)
              }
            case Some(_) => Forbidden(Error.forbidden)
            case None    => Unauthorized(Error.unauthorized)
          }
        }
    }
  }
}
