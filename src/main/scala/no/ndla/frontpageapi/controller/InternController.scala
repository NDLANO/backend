/*
 * Part of NDLA frontpage_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.controller

import cats.Monad
import cats.effect.{Effect, IO}
import no.ndla.frontpageapi.model.api._
import no.ndla.frontpageapi.model.domain.Errors.{NotFoundException, ValidationError}
import no.ndla.frontpageapi.service.{ReadService, WriteService}
import org.http4s.rho.RhoService
import org.http4s.rho.swagger.SwaggerSyntax

import scala.language.higherKinds
import scala.util.{Failure, Success}

trait InternController {
  this: ReadService with WriteService =>
  val internController: InternController[IO]

  class InternController[F[+ _]: Effect](swaggerSyntax: SwaggerSyntax[F])(implicit F: Monad[F]) extends RhoService[F] {
    import swaggerSyntax._

    "Get subject page id from external id" **
      GET / "subjectpage" / "external" / pathVar[String]("externalId", "old NDLA node id") |>> { nid: String =>
      {
        readService.getIdFromExternalId(nid) match {
          case Success(Some(id)) => Ok(id)
          case Success(None)     => NotFound(Error.notFound)
          case Failure(_)        => InternalServerError(Error.generic)
        }
      }
    }

    "Create new subject page" **
      POST / "subjectpage" ^ NewOrUpdateSubjectFrontPageData.decoder |>> {
      subjectPage: NewOrUpdateSubjectFrontPageData =>
        {
          writeService.newSubjectPage(subjectPage) match {
            case Success(s)                   => Ok(s)
            case Failure(ex: ValidationError) => BadRequest(Error.badRequest(ex.getMessage))
            case Failure(_)                   => InternalServerError(Error.generic)
          }
        }
    }

    "Update subject page" **
      PUT / "subjectpage" / pathVar[Long]("subject-id", "The subject id") ^ NewOrUpdateSubjectFrontPageData.decoder |>> {
      (id: Long, subjectPage: NewOrUpdateSubjectFrontPageData) =>
        {
          writeService.updateSubjectPage(id, subjectPage) match {
            case Success(s)                    => Ok(s)
            case Failure(_: NotFoundException) => NotFound(Error.notFound)
            case Failure(ex: ValidationError)  => BadRequest(Error.badRequest(ex.getMessage))
            case Failure(_)                    => InternalServerError(Error.generic)
          }
        }
    }

    "Update front page" **
      POST / "frontpage" ^ FrontPageData.decoder |>> { frontPage: FrontPageData =>
      {
        writeService.updateFrontPage(frontPage) match {
          case Success(s) => Ok(s)
          case Failure(_) => InternalServerError(Error.generic)
        }
      }
    }

  }
}
