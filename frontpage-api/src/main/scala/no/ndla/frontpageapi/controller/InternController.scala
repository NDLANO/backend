/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.controller

import cats.implicits._
import io.circe.generic.auto._
import no.ndla.frontpageapi.{Eff, Props}
import no.ndla.frontpageapi.model.api._
import no.ndla.frontpageapi.service.{ReadService, WriteService}
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.Service
import no.ndla.network.tapir.TapirErrors.errorOutputsFor
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.server.ServerEndpoint

import scala.util.{Failure, Success}

trait InternController {
  this: ReadService with WriteService with Props with ErrorHelpers =>
  val internController: InternController

  class InternController extends Service[Eff] {
    override val prefix: EndpointInput[Unit] = "intern"
    override val enableSwagger               = false

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      endpoint.get
        .in("subjectpage" / "external" / path[String]("externalId").description("old NDLA node id"))
        .summary("Get subject page id from external id")
        .out(jsonBody[SubjectPageId])
        .errorOut(errorOutputsFor(400, 404))
        .serverLogicPure { nid =>
          readService.getIdFromExternalId(nid) match {
            case Success(Some(id)) => id.asRight
            case Success(None)     => ErrorHelpers.notFound.asLeft
            case Failure(ex)       => returnLeftError(ex)
          }
        },
      endpoint.post
        .summary("Create new subject page")
        .in("subjectpage")
        .in(jsonBody[NewSubjectFrontPageData])
        .errorOut(errorOutputsFor())
        .out(jsonBody[SubjectPageData])
        .serverLogicPure { subjectPage =>
          writeService
            .newSubjectPage(subjectPage)
            .handleErrorsOrOk
        },
      endpoint.put
        .in("subjectpage" / path[Long]("subject-id").description("The subject id"))
        .in(jsonBody[NewSubjectFrontPageData])
        .errorOut(errorOutputsFor(400, 404))
        .summary("Update subject page")
        .out(jsonBody[SubjectPageData])
        .serverLogicPure { case (id, subjectPage) =>
          writeService
            .updateSubjectPage(id, subjectPage, props.DefaultLanguage)
            .handleErrorsOrOk
        }
    )
  }
}
