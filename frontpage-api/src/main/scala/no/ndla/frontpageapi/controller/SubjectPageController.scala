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
import no.ndla.frontpageapi.auth.UserInfo
import no.ndla.frontpageapi.model.api.{
  Error,
  ErrorHelpers,
  NewSubjectFrontPageData,
  SubjectPageData,
  UpdatedSubjectFrontPageData
}
import no.ndla.frontpageapi.model.domain.Errors.{NotFoundException, ValidationException}
import no.ndla.frontpageapi.service.{ReadService, WriteService}
import no.ndla.frontpageapi.Props
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.model.CommaSeparated
import sttp.tapir.server.ServerEndpoint

import scala.util.{Failure, Success, Try}

trait SubjectPageController {
  this: ReadService with WriteService with Props with ErrorHelpers with Service =>
  val subjectPageController: SubjectPageController

  class SubjectPageController extends SwaggerService {
    override val prefix: EndpointInput[Unit] = "frontpage-api" / "v1" / "subjectpage"

    private val errorOutputs = oneOf[Error](
      oneOfVariant(NotFoundError),
      oneOfVariant(GenericError),
      oneOfDefaultVariant(GenericError)
    )

    implicit class handleErrorOrOkClass[T](t: Try[T]) {
      def handleErrorsOrOk: Either[Error, T] = {
        t match {
          case Success(value) => value.asRight
          case Failure(ex)    => ErrorHelpers.returnError(ex).asLeft
        }
      }
    }

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
          readService.subjectPage(id, language, fallback) match {
            case Some(s) => s.asRight
            case None    => ErrorHelpers.notFound.asLeft
          }
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
        .errorOut(
          oneOf(oneOfVariant(GenericError), oneOfVariant(ForbiddenError), oneOfVariant(UnprocessableEntityError))
        )
        .serverSecurityLogicPure {
          case Some(user) if user.canWrite => user.asRight
          case Some(_)                     => ErrorHelpers.forbidden.asLeft
          case None                        => ErrorHelpers.unauthorized.asLeft
        }
        .serverLogicPure { _ => newSubjectFrontPageData =>
          {
            writeService.newSubjectPage(newSubjectFrontPageData) match {
              case Success(s)                       => s.asRight
              case Failure(ex: ValidationException) => ErrorHelpers.unprocessableEntity(ex.getMessage).asLeft
              case Failure(_)                       => ErrorHelpers.generic.asLeft
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
        .errorOut(
          oneOf(oneOfVariant(GenericError), oneOfVariant(ForbiddenError), oneOfVariant(UnprocessableEntityError))
        )
        .serverSecurityLogicPure {
          case Some(user) if user.canWrite => user.asRight
          case Some(_)                     => ErrorHelpers.forbidden.asLeft
          case None                        => ErrorHelpers.unauthorized.asLeft
        }
        .serverLogicPure { _ =>
          { case (subjectPage, id, language, fallback) =>
            writeService.updateSubjectPage(id, subjectPage, language) match {
              case Success(s)                       => s.asRight
              case Failure(_: NotFoundException)    => ErrorHelpers.notFound.asLeft
              case Failure(ex: ValidationException) => ErrorHelpers.unprocessableEntity(ex.getMessage).asLeft
              case Failure(_)                       => ErrorHelpers.generic.asLeft
            }
          }

        }
    )
  }
}
