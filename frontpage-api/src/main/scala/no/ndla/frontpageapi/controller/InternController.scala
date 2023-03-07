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
import no.ndla.frontpageapi.model.api.SubjectPageId._
import no.ndla.frontpageapi.model.api.NewOrUpdatedAboutSubject._
import no.ndla.frontpageapi.service.{ReadService, WriteService}
import no.ndla.frontpageapi.Props
import io.circe.generic.auto._
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint

import scala.util.{Failure, Success}

trait InternController {
  this: ReadService with WriteService with Props with ErrorHelpers with Service =>
  val internController: InternController

  import ErrorHelpers.handleErrorOrOkClass

  class InternController extends SwaggerService {
    override val prefix        = "intern"
    override val enableSwagger = false

    override val endpoints: List[ServerEndpoint[Any, IO]] = List(
      endpoint.get
        .in("subjectpage" / "external" / path[String]("externalId").description("old NDLA node id"))
        .summary("Get subject page id from external id")
        .out(jsonBody[SubjectPageId])
        .errorOut(errorOutputs)
        .serverLogicPure { nid =>
          readService.getIdFromExternalId(nid) match {
            case Success(Some(id)) => id.asRight
            case Success(None)     => ErrorHelpers.notFound.asLeft
            case Failure(ex)       => ErrorHelpers.returnError(ex).asLeft
          }
        },
      endpoint.post
        .summary("Create new subject page")
        .in("subjectpage")
        .in(jsonBody[NewSubjectFrontPageData])
        .errorOut(errorOutputs)
        .out(jsonBody[SubjectPageData])
        .serverLogicPure { subjectPage =>
          writeService
            .newSubjectPage(subjectPage)
            .handleErrorsOrOk
        },
      endpoint.put
        .in("subjectpage" / path[Long]("subject-id").description("The subject id"))
        .in(jsonBody[NewSubjectFrontPageData])
        .errorOut(errorOutputs)
        .summary("Update subject page")
        .out(jsonBody[SubjectPageData])
        .serverLogicPure { case (id, subjectPage) =>
          writeService
            .updateSubjectPage(id, subjectPage, props.DefaultLanguage)
            .handleErrorsOrOk
        },
      endpoint.post
        .summary("Update front page")
        .in(jsonBody[FrontPageData])
        .errorOut(errorOutputs)
        .out(jsonBody[FrontPageData])
        .serverLogicPure { frontPage =>
          writeService
            .updateFrontPage(frontPage)
            .handleErrorsOrOk
        }
    )
  }
}
