/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.controller

import cats.effect.{Effect, IO}
import no.ndla.frontpageapi.Props
import no.ndla.frontpageapi.model.api._
import no.ndla.frontpageapi.model.domain.Errors.{NotFoundException, ValidationException}
import no.ndla.frontpageapi.service.{ReadService, WriteService}
import org.http4s.rho.RhoRoutes
import org.http4s.rho.swagger.SwaggerSyntax

import scala.util.{Failure, Success}

trait InternController {
  this: ReadService with WriteService with Props with ErrorHelpers =>
  val internController: InternController[IO]

  class InternController[F[+_]: Effect](swaggerSyntax: SwaggerSyntax[F]) extends RhoRoutes[F] {
    import swaggerSyntax._

    "Get subject page id from external id" **
      GET / "subjectpage" / "external" / pathVar[String]("externalId", "old NDLA node id") |>> { nid: String =>
        {
          readService.getIdFromExternalId(nid) match {
            case Success(Some(id)) => Ok(id)
            case Success(None)     => NotFound(ErrorHelpers.notFound)
            case Failure(_)        => InternalServerError(ErrorHelpers.generic)
          }
        }
      }

    "Create new subject page" **
      POST / "subjectpage" ^ NewSubjectFrontPageData.decoder |>> { subjectPage: NewSubjectFrontPageData =>
        {
          writeService.newSubjectPage(subjectPage) match {
            case Success(s)                       => Ok(s)
            case Failure(ex: ValidationException) => BadRequest(ErrorHelpers.badRequest(ex.getMessage))
            case Failure(_)                       => InternalServerError(ErrorHelpers.generic)
          }
        }
      }

    "Update subject page" **
      PUT / "subjectpage" / pathVar[Long]("subject-id", "The subject id") ^ NewSubjectFrontPageData.decoder |>> {
        (id: Long, subjectPage: NewSubjectFrontPageData) =>
          {
            writeService.updateSubjectPage(id, subjectPage, props.DefaultLanguage) match {
              case Success(s)                       => Ok(s)
              case Failure(_: NotFoundException)    => NotFound(ErrorHelpers.notFound)
              case Failure(ex: ValidationException) => BadRequest(ErrorHelpers.badRequest(ex.getMessage))
              case Failure(_)                       => InternalServerError(ErrorHelpers.generic)
            }
          }
      }

    "Update front page" **
      POST / "frontpage" ^ FrontPageData.decoder |>> { frontPage: FrontPageData =>
        {
          writeService.updateFrontPage(frontPage) match {
            case Success(s) => Ok(s)
            case Failure(_) => InternalServerError(ErrorHelpers.generic)
          }
        }
      }

  }
}
