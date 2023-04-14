/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.controller

import cats.effect.IO
import cats.implicits._
import io.circe.generic.auto._
import no.ndla.frontpageapi.Props
import no.ndla.frontpageapi.auth.UserInfo
import no.ndla.frontpageapi.model.api.{ErrorHelpers, NewSubjectFrontPageData, SubjectPageData, UpdatedSubjectFrontPageData}
import no.ndla.frontpageapi.model.domain.Errors.ValidationException
import no.ndla.frontpageapi.service.{ReadService, WriteService}
import no.ndla.network.tapir.Service
import no.ndla.network.tapir.TapirErrors.errorOutputs
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

    import UserInfo._
    override val endpoints: List[ServerEndpoint[Any, IO]] = List(
      endpoint.get
        .summary("Fetch all subjectpages")
        .in(query[Int]("page").default(1))
        .in(query[Int]("page-size").default(props.DefaultPageSize))
        .in(query[String]("language").default(props.DefaultLanguage))
        .in(query[Boolean]("fallback").default(false))
        .errorOut(errorOutputs)
        .out(jsonBody[List[SubjectPageData]])
        .serverLogicPure { case (page, pageSize, language, fallback) =>
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
        .errorOut(errorOutputs)
        .serverLogicPure { case (id, language, fallback) =>
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
        .errorOut(errorOutputs)
        .serverLogicPure { case (ids, language, fallback, pageSize, page) =>
          val parsedPageSize = if (pageSize < 1) props.DefaultPageSize else pageSize
          val parsedPage     = if (page < 1) 1 else page
          readService
            .getSubjectPageByIds(ids.values, language, fallback, parsedPageSize, parsedPage)
            .handleErrorsOrOk
        },
      endpoint.post
        .summary("Create new subject page")
        .securityIn(auth.bearer[Option[UserInfo]]())
        .in(jsonBody[NewSubjectFrontPageData])
        .out(jsonBody[SubjectPageData])
        .errorOut(errorOutputs)
        .serverSecurityLogicPure {
          case Some(user) if user.canWrite => user.asRight
          case Some(_)                     => ErrorHelpers.forbidden.asLeft
          case None                        => ErrorHelpers.unauthorized.asLeft
        }
        .serverLogicPure { _ => newSubjectFrontPageData =>
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
        .securityIn(auth.bearer[Option[UserInfo]]())
        .in(jsonBody[UpdatedSubjectFrontPageData])
        .in(path[Long]("subjectpage-id").description("The subjectpage id"))
        .in(query[String]("language").default(props.DefaultLanguage))
        .in(query[Boolean]("fallback").default(false))
        .out(jsonBody[SubjectPageData])
        .errorOut(errorOutputs)
        .serverSecurityLogicPure {
          case Some(user) if user.canWrite => user.asRight
          case Some(_)                     => ErrorHelpers.forbidden.asLeft
          case None                        => ErrorHelpers.unauthorized.asLeft
        }
        .serverLogicPure { _ =>
          { case (subjectPage, id, language, fallback) =>
            writeService
              .updateSubjectPage(id, subjectPage, language)
              .partialOverride { case ex: ValidationException =>
                ErrorHelpers.unprocessableEntity(ex.getMessage)
              }
          }
        }
    )
  }
}
