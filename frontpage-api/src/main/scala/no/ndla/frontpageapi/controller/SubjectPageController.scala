/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.controller

import cats.effect.IO
import io.circe.generic.auto._
import no.ndla.frontpageapi.Props
import no.ndla.frontpageapi.model.api.{
  ErrorHelpers,
  NewSubjectFrontPageData,
  SubjectPageData,
  UpdatedSubjectFrontPageData
}
import no.ndla.frontpageapi.model.domain.Errors.ValidationException
import no.ndla.frontpageapi.service.{ReadService, WriteService}
import no.ndla.network.tapir.Service
import no.ndla.network.tapir.TapirErrors.errorOutputsFor
import no.ndla.network.tapir.auth.Permission.FRONTPAGE_API_WRITE
import no.ndla.network.tapir.auth.TokenUser
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.model.CommaSeparated
import sttp.tapir.server.ServerEndpoint

trait SubjectPageController {
  this: ReadService with WriteService with Props with ErrorHelpers with Service =>
  val subjectPageController: SubjectPageController

  class SubjectPageController extends SwaggerService {
    override val prefix: EndpointInput[Unit] = "frontpage-api" / "v1" / "subjectpage"

    import ErrorHelpers._
    override val endpoints: List[ServerEndpoint[Any, IO]] = List(
      endpoint.get
        .summary("Fetch all subjectpages")
        .in(query[Int]("page").default(1))
        .in(query[Int]("page-size").default(props.DefaultPageSize))
        .in(query[String]("language").default(props.DefaultLanguage))
        .in(query[Boolean]("fallback").default(false))
        .errorOut(errorOutputsFor(400, 404))
        .out(jsonBody[List[SubjectPageData]])
        .serverLogic { case (page, pageSize, language, fallback) =>
          readService
            .subjectPages(page, pageSize, language, fallback)
            .handleErrorsOrOk
        },
      endpoint.get
        .summary("Get data to display on a subject page")
        .in(path[Long]("subjectpage-id").description("The subjectpage id"))
        .in(query[String]("language").default(props.DefaultLanguage))
        .in(query[Boolean]("fallback").default(false))
        .out(jsonBody[SubjectPageData])
        .errorOut(errorOutputsFor(400, 404))
        .serverLogic { case (id, language, fallback) =>
          readService
            .subjectPage(id, language, fallback)
            .handleErrorsOrOk
        },
      endpoint.get
        .summary("Fetch subject pages that matches ids parameter")
        .in("ids")
        .in(query[CommaSeparated[Long]]("ids"))
        .in(query[String]("language").default(props.DefaultLanguage))
        .in(query[Boolean]("fallback").default(false))
        .in(query[Int]("page-size").default(props.DefaultPageSize))
        .in(query[Int]("page").default(1))
        .out(jsonBody[List[SubjectPageData]])
        .errorOut(errorOutputsFor(400, 404))
        .serverLogic { case (ids, language, fallback, pageSize, page) =>
          val parsedPageSize = if (pageSize < 1) props.DefaultPageSize else pageSize
          val parsedPage     = if (page < 1) 1 else page
          readService
            .getSubjectPageByIds(ids.values, language, fallback, parsedPageSize, parsedPage)
            .handleErrorsOrOk
        },
      endpoint.post
        .summary("Create new subject page")
        .in(jsonBody[NewSubjectFrontPageData])
        .out(jsonBody[SubjectPageData])
        .errorOut(errorOutputsFor(400, 404))
        .securityIn(auth.bearer[Option[TokenUser]]())
        .serverSecurityLogicPure(requireScope(FRONTPAGE_API_WRITE))
        .serverLogic { _ => newSubjectFrontPageData =>
          {
            writeService
              .newSubjectPage(newSubjectFrontPageData)
              .partialOverride { case ex: ValidationException =>
                ErrorHelpers.unprocessableEntity(ex.getMessage)
              }
          }
        },
      endpoint.patch
        .summary("Update subject page")
        .in(jsonBody[UpdatedSubjectFrontPageData])
        .in(path[Long]("subjectpage-id").description("The subjectpage id"))
        .in(query[String]("language").default(props.DefaultLanguage))
        .in(query[Boolean]("fallback").default(false))
        .out(jsonBody[SubjectPageData])
        .errorOut(errorOutputsFor(400, 404))
        .securityIn(auth.bearer[Option[TokenUser]]())
        .serverSecurityLogicPure(requireScope(FRONTPAGE_API_WRITE))
        .serverLogic { _ =>
          { case (subjectPage, id, language, fallback) =>
            writeService
              .updateSubjectPage(id, subjectPage, language, fallback)
              .partialOverride { case ex: ValidationException =>
                ErrorHelpers.unprocessableEntity(ex.getMessage)
              }
          }
        }
    )
  }
}
