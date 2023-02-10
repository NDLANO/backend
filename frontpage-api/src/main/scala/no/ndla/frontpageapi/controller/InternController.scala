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
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint

import scala.util.{Failure, Success, Try}

trait InternController {
  this: ReadService with WriteService with Props with ErrorHelpers with Service =>
  val internController: InternController

  class InternController extends SwaggerService {
    override val prefix        = "intern"
    override val enableSwagger = false

    implicit class handleErrorOrOkClass[T](t: Try[T]) {
      def handleErrorsOrOk: Either[Error, T] = {
        t match {
          case Success(value) => value.asRight
          case Failure(ex)    => ErrorHelpers.returnError(ex).asLeft
        }
      }
    }

    override val endpoints: List[ServerEndpoint[Any, IO]] = List(
      endpoint.get
        .in("subjectpage" / "external" / path[String]("externalId").description("old NDLA node id"))
        .summary("Get subject page id from external id")
        .out(jsonBody[SubjectPageId])
        .errorOut(oneOf[Error](oneOfVariant(GenericError), oneOfVariant(NotFoundError)))
        .serverLogicPure { nid => readService.getIdFromExternalId(nid).handleErrorsOrOk },
      endpoint.post
        .summary("Create new subject page")
        .in("subjectpage")
        .in(jsonBody[NewSubjectFrontPageData])
        .errorOut(oneOf[Error](oneOfVariant(BadRequestError), oneOfVariant(GenericError)))
        .out(jsonBody[SubjectPageData])
        .serverLogicPure { subjectPage => writeService.newSubjectPage(subjectPage).handleErrorsOrOk },
      endpoint.put
        .in("subjectpage" / path[Long]("subject-id").description("The subject id"))
        .in(jsonBody[NewSubjectFrontPageData])
        .in(query[Boolean]("fallback").default(false))
        .errorOut(oneOf[Error](oneOfVariant(BadRequestError), oneOfVariant(GenericError), oneOfVariant(NotFoundError)))
        .summary("Update subject page")
        .out(jsonBody[SubjectPageData])
        .serverLogicPure { case (id, subjectPage, fallback) =>
          writeService.updateSubjectPage(id, subjectPage, props.DefaultLanguage, fallback).handleErrorsOrOk
        },
      endpoint.post
        .summary("Update front page")
        .in(jsonBody[FrontPageData])
        .errorOut(oneOf[Error](oneOfVariant(GenericError)))
        .out(jsonBody[FrontPageData])
        .serverLogicPure { frontPage => writeService.updateFrontPage(frontPage).handleErrorsOrOk }
    )
  }
}
