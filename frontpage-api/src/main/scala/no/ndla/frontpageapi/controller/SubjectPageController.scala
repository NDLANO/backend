/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.controller

import io.circe.generic.auto.*
import no.ndla.common.model.api.CommaSeparatedList.*
import no.ndla.frontpageapi.Props
import no.ndla.frontpageapi.model.api.{
  ErrorHelpers,
  NewSubjectFrontPageData,
  SubjectPageData,
  UpdatedSubjectFrontPageData
}
import no.ndla.frontpageapi.model.domain.Errors.ValidationException
import no.ndla.frontpageapi.service.{ReadService, WriteService}
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.TapirController
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import no.ndla.network.tapir.auth.Permission.FRONTPAGE_API_WRITE
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint

trait SubjectPageController {
  this: ReadService & WriteService & Props & ErrorHelpers & TapirController =>
  val subjectPageController: SubjectPageController

  class SubjectPageController extends TapirController {
    override val serviceName: String         = "subjectpage"
    override val prefix: EndpointInput[Unit] = "frontpage-api" / "v1" / serviceName

    import ErrorHelpers._

    def getAllSubjectPages: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Fetch all subjectpages")
      .in(query[Int]("page").default(1).validate(Validator.min(1)))
      .in(query[Int]("page-size").default(props.DefaultPageSize).validate(Validator.min(0)))
      .in(query[String]("language").default(props.DefaultLanguage))
      .in(query[Boolean]("fallback").default(false))
      .errorOut(errorOutputsFor(400, 404))
      .out(jsonBody[List[SubjectPageData]])
      .serverLogicPure { case (page, pageSize, language, fallback) =>
        readService
          .subjectPages(page, pageSize, language, fallback)

      }

    def getSingleSubjectPage: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Get data to display on a subject page")
      .in(path[Long]("subjectpage-id").description("The subjectpage id"))
      .in(query[String]("language").default(props.DefaultLanguage))
      .in(query[Boolean]("fallback").default(false))
      .out(jsonBody[SubjectPageData])
      .errorOut(errorOutputsFor(400, 404))
      .serverLogicPure { case (id, language, fallback) =>
        readService
          .subjectPage(id, language, fallback)

      }

    def getSubjectPagesByIds: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Fetch subject pages that matches ids parameter")
      .in("ids")
      .in(listQuery[Long]("ids"))
      .in(query[String]("language").default(props.DefaultLanguage))
      .in(query[Boolean]("fallback").default(false))
      .in(query[Int]("page-size").default(props.DefaultPageSize))
      .in(query[Int]("page").default(1))
      .out(jsonBody[List[SubjectPageData]])
      .errorOut(errorOutputsFor(400, 404))
      .serverLogicPure { case (ids, language, fallback, pageSize, page) =>
        val parsedPageSize = if (pageSize < 1) props.DefaultPageSize else pageSize
        val parsedPage     = if (page < 1) 1 else page
        readService
          .getSubjectPageByIds(ids.values, language, fallback, parsedPageSize, parsedPage)

      }

    def createNewSubjectPage: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Create new subject page")
      .in(jsonBody[NewSubjectFrontPageData])
      .out(jsonBody[SubjectPageData])
      .errorOut(errorOutputsFor(400, 404))
      .requirePermission(FRONTPAGE_API_WRITE)
      .serverLogicPure { _ => newSubjectFrontPageData =>
        {
          writeService
            .newSubjectPage(newSubjectFrontPageData)
            .partialOverride { case ex: ValidationException =>
              ErrorHelpers.unprocessableEntity(ex.getMessage)
            }
        }
      }
    def updateSubjectPage: ServerEndpoint[Any, Eff] = endpoint.patch
      .summary("Update subject page")
      .in(jsonBody[UpdatedSubjectFrontPageData])
      .in(path[Long]("subjectpage-id").description("The subjectpage id"))
      .in(query[String]("language").default(props.DefaultLanguage))
      .in(query[Boolean]("fallback").default(false))
      .out(jsonBody[SubjectPageData])
      .errorOut(errorOutputsFor(400, 404))
      .requirePermission(FRONTPAGE_API_WRITE)
      .serverLogicPure { _ =>
        { case (subjectPage, id, language, fallback) =>
          writeService
            .updateSubjectPage(id, subjectPage, language, fallback)
            .partialOverride { case ex: ValidationException =>
              ErrorHelpers.unprocessableEntity(ex.getMessage)
            }
        }
      }

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      getAllSubjectPages,
      getSubjectPagesByIds,
      getSingleSubjectPage,
      createNewSubjectPage,
      updateSubjectPage
    )
  }
}
