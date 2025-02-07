/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.controller

import cats.implicits.*
import io.circe.generic.auto.*
import no.ndla.frontpageapi.Props
import no.ndla.frontpageapi.model.api.*
import no.ndla.frontpageapi.service.{ReadService, WriteService}
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.TapirController
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint

import scala.util.{Failure, Success}

trait InternController {
  this: ReadService & WriteService & Props & ErrorHandling & TapirController =>
  val internController: InternController

  class InternController extends TapirController {
    override val prefix: EndpointInput[Unit] = "intern"
    override val enableSwagger               = false

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      endpoint.get
        .in("subjectpage" / "external" / path[String]("externalId").description("old NDLA node id"))
        .summary("Get subject page id from external id")
        .out(jsonBody[SubjectPageIdDTO])
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
        .in(jsonBody[NewSubjectFrontPageDataDTO])
        .errorOut(errorOutputsFor())
        .out(jsonBody[SubjectPageDataDTO])
        .serverLogicPure { subjectPage =>
          writeService
            .newSubjectPage(subjectPage)

        },
      endpoint.put
        .in("subjectpage" / path[Long]("subject-id").description("The subject id"))
        .in(jsonBody[NewSubjectFrontPageDataDTO])
        .errorOut(errorOutputsFor(400, 404))
        .summary("Update subject page")
        .out(jsonBody[SubjectPageDataDTO])
        .serverLogicPure { case (id, subjectPage) =>
          writeService
            .updateSubjectPage(id, subjectPage, props.DefaultLanguage)

        }
    )
  }
}
