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
import no.ndla.frontpageapi.Props
import io.circe.generic.auto._
import no.ndla.frontpageapi.model.domain.Errors.{NotFoundException, ValidationException}
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint

import scala.util.{Failure, Success}

trait InternController {
  this: ReadService with WriteService with Props with ErrorHelpers with Service =>
  val internController: InternController

  class InternController extends SwaggerService {
    override val prefix        = "intern"
    override val enableSwagger = false

    override val endpoints: List[ServerEndpoint[Any, IO]] = List(
      endpoint.get
        .in("subjectpage" / "external" / path[String]("externalId").description("old NDLA node id"))
        .summary("Get subject page id from external id")
        .out(jsonBody[SubjectPageId])
        .errorOut(oneOf(oneOfVariant(GenericError), oneOfVariant(NotFoundError)))
        .serverLogicPure { nid =>
          readService.getIdFromExternalId(nid) match {
            case Success(Some(id)) => id.asRight
            case Success(None)     => ErrorHelpers.notFound.asLeft
            case Failure(_)        => ErrorHelpers.generic.asLeft
          }
        },
      endpoint.post
        .summary("Create new subject page")
        .in("subjectpage")
        .in(jsonBody[NewSubjectFrontPageData])
        .errorOut(oneOf(oneOfVariant(BadRequestError), oneOfVariant(GenericError)))
        .out(jsonBody[SubjectPageData])
        .serverLogicPure { subjectPage =>
          writeService.newSubjectPage(subjectPage) match {
            case Success(s)                       => s.asRight
            case Failure(ex: ValidationException) => ErrorHelpers.badRequest(ex.getMessage).asLeft
            case Failure(_)                       => ErrorHelpers.generic.asLeft
          }
        },
      endpoint.put
        .in("subjectpage" / path[Long]("subject-id").description("The subject id"))
        .in(jsonBody[NewSubjectFrontPageData])
        .errorOut(oneOf(oneOfVariant(BadRequestError), oneOfVariant(GenericError), oneOfVariant(NotFoundError)))
        .summary("Update subject page")
        .out(jsonBody[SubjectPageData])
        .serverLogicPure { case (id, subjectPage) =>
          writeService.updateSubjectPage(id, subjectPage, props.DefaultLanguage) match {
            case Success(s)                       => s.asRight
            case Failure(_: NotFoundException)    => ErrorHelpers.notFound.asLeft
            case Failure(ex: ValidationException) => ErrorHelpers.badRequest(ex.getMessage).asLeft
            case Failure(_)                       => ErrorHelpers.generic.asLeft
          }
        },
      endpoint.post
        .summary("Update front page")
        .in(jsonBody[FrontPageData])
        .errorOut(oneOf(oneOfVariant(GenericError)))
        .out(jsonBody[FrontPageData])
        .serverLogicPure { frontPage =>
          writeService.updateFrontPage(frontPage) match {
            case Success(s) => s.asRight
            case Failure(_) => ErrorHelpers.generic.asLeft
          }
        }
    )
  }
}
