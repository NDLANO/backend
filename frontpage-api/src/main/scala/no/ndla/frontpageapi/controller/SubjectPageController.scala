/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.controller

import cats.effect.{Effect, IO}
import no.ndla.frontpageapi.Props
import no.ndla.frontpageapi.auth.UserInfo
import no.ndla.frontpageapi.model.api.{ErrorHelpers, NewSubjectFrontPageData, UpdatedSubjectFrontPageData}
import no.ndla.frontpageapi.model.domain.Errors.{NotFoundException, ValidationException}
import no.ndla.frontpageapi.service.{ReadService, WriteService}
import org.http4s.rho.swagger.{SecOps, SwaggerSyntax}

import scala.util.{Failure, Success}

trait SubjectPageController {
  this: ReadService with WriteService with Props with ErrorHelpers =>
  val subjectPageController: SubjectPageController[IO]

  class SubjectPageController[F[+_]: Effect](swaggerSyntax: SwaggerSyntax[F]) extends AuthController[F] {

    import swaggerSyntax._

    "Get data to display on a subject page" **
      GET / pathVar[Long]("subjectpage-id", "The subjectpage id") +? param[String](
        "language",
        props.DefaultLanguage
      ) & param[Boolean]("fallback", false) |>> { (id: Long, language: String, fallback: Boolean) =>
        {
          readService.subjectPage(id, language, fallback) match {
            case Some(s) => Ok(s)
            case None    => NotFound(ErrorHelpers.notFound)
          }
        }
      }

    AuthOptions.^^("Create new subject page" ** POST) >>> Auth.auth ^ NewSubjectFrontPageData.decoder |>> {
      (user: Option[UserInfo], newSubjectFrontPageData: NewSubjectFrontPageData) =>
        {
          user match {
            case Some(user) if user.canWrite =>
              writeService.newSubjectPage(newSubjectFrontPageData) match {
                case Success(s) => Ok(s)
                case Failure(ex: ValidationException) =>
                  UnprocessableEntity(ErrorHelpers.unprocessableEntity(ex.getMessage))
                case Failure(_) => InternalServerError(ErrorHelpers.generic)
              }
            case Some(_) => Forbidden(ErrorHelpers.forbidden)
            case None    => Unauthorized(ErrorHelpers.unauthorized)
          }
        }
    }

    AuthOptions.^^(
      "Update subject page" **
        PATCH
    ) / pathVar[Long]("subjectpage-id", "The subjectpage id") +? param[String](
      "language",
      props.DefaultLanguage
    ) >>> Auth.auth ^ UpdatedSubjectFrontPageData.decoder |>> {
      (id: Long, language: String, user: Option[UserInfo], subjectPage: UpdatedSubjectFrontPageData) =>
        {
          user match {
            case Some(user) if user.canWrite =>
              writeService.updateSubjectPage(id, subjectPage, language) match {
                case Success(s)                    => Ok(s)
                case Failure(_: NotFoundException) => NotFound(ErrorHelpers.notFound)
                case Failure(ex: ValidationException) =>
                  UnprocessableEntity(ErrorHelpers.unprocessableEntity(ex.getMessage))
                case Failure(_) => InternalServerError(ErrorHelpers.generic)
              }
            case Some(_) => Forbidden(ErrorHelpers.forbidden)
            case None    => Unauthorized(ErrorHelpers.unauthorized)
          }
        }
    }
  }
}
