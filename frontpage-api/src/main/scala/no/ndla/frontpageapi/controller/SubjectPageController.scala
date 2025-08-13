/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.controller

import no.ndla.common.errors.ValidationException
import no.ndla.common.model.api.CommaSeparatedList.*
import no.ndla.common.model.api.LanguageCode
import no.ndla.common.model.api.frontpage.SubjectPageDTO
import no.ndla.frontpageapi.Props
import no.ndla.frontpageapi.model.api.{ErrorHandling, NewSubjectPageDTO, UpdatedSubjectPageDTO}
import no.ndla.frontpageapi.service.{ReadService, WriteService}
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.TapirController
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import no.ndla.network.tapir.auth.Permission.FRONTPAGE_API_WRITE
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint

trait SubjectPageController {
  this: ReadService & WriteService & Props & ErrorHandling & TapirController =>
  lazy val subjectPageController: SubjectPageController

  class SubjectPageController extends TapirController {
    override val serviceName: String         = "subjectpage"
    override val prefix: EndpointInput[Unit] = "frontpage-api" / "v1" / serviceName

    private val pathSubjectPageId = path[Long]("subjectpage-id").description("The subjectpage id")
    private val pathLanguage      = path[LanguageCode]("language")
      .description("The ISO 639-1 language code describing language.")
    private val pageNo = query[Int]("page")
      .description("The page number of the search hits to display.")
      .default(1)
      .validate(Validator.min(1))
    private val pageSize = query[Int]("page-size")
      .description("The number of search hits to display for each page.")
      .default(props.DefaultPageSize)
      .validate(Validator.min(0))
    private val ids = listQuery[Long]("ids")
      .description(
        "Return only subject pages that have one of the provided ids. To provide multiple ids, separate by comma (,)."
      )
    private val language = query[LanguageCode]("language")
      .description("The ISO 639-1 language code describing language.")
      .default(LanguageCode(props.DefaultLanguage))
    private val fallback = query[Boolean]("fallback")
      .description("Fallback to existing language if language is specified.")
      .default(false)

    def getAllSubjectPages: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Fetch all subjectpages")
      .in(pageNo)
      .in(pageSize)
      .in(language)
      .in(fallback)
      .errorOut(errorOutputsFor(400, 404))
      .out(jsonBody[List[SubjectPageDTO]])
      .serverLogicPure { case (page, pageSize, language, fallback) =>
        readService.subjectPages(page, pageSize, language.code, fallback)
      }

    def getSingleSubjectPage: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Get data to display on a subject page")
      .in(pathSubjectPageId)
      .in(language)
      .in(fallback)
      .out(jsonBody[SubjectPageDTO])
      .errorOut(errorOutputsFor(400, 404))
      .serverLogicPure { case (id, language, fallback) =>
        readService.subjectPage(id, language.code, fallback)
      }

    def getSubjectPagesByIds: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Fetch subject pages that matches ids parameter")
      .in("ids")
      .in(ids)
      .in(language)
      .in(fallback)
      .in(pageSize)
      .in(pageNo)
      .out(jsonBody[List[SubjectPageDTO]])
      .errorOut(errorOutputsFor(400, 404))
      .serverLogicPure { case (ids, language, fallback, pageSize, page) =>
        val parsedPageSize = if (pageSize < 1) props.DefaultPageSize else pageSize
        val parsedPage     = if (page < 1) 1 else page
        readService.getSubjectPageByIds(ids.values, language.code, fallback, parsedPageSize, parsedPage)

      }

    def createNewSubjectPage: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Create new subject page")
      .in(jsonBody[NewSubjectPageDTO])
      .out(jsonBody[SubjectPageDTO])
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
      .in(jsonBody[UpdatedSubjectPageDTO])
      .in(pathSubjectPageId)
      .in(language)
      .in(fallback)
      .out(jsonBody[SubjectPageDTO])
      .errorOut(errorOutputsFor(400, 404))
      .requirePermission(FRONTPAGE_API_WRITE)
      .serverLogicPure { _ =>
        { case (subjectPage, id, language, fallback) =>
          writeService
            .updateSubjectPage(id, subjectPage, language.code, fallback)
            .partialOverride { case ex: ValidationException =>
              ErrorHelpers.unprocessableEntity(ex.getMessage)
            }
        }
      }

    def deleteLanguage: ServerEndpoint[Any, Eff] = endpoint.delete
      .in(pathSubjectPageId / "language" / pathLanguage)
      .summary("Delete language from subject page")
      .description("Delete language from subject page")
      .out(jsonBody[SubjectPageDTO])
      .errorOut(errorOutputsFor(400, 401, 403, 404))
      .requirePermission(FRONTPAGE_API_WRITE)
      .serverLogicPure { _ =>
        { case (articleId, language) =>
          writeService.deleteSubjectPageLanguage(articleId, language.code)
        }
      }

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      getAllSubjectPages,
      getSubjectPagesByIds,
      getSingleSubjectPage,
      createNewSubjectPage,
      updateSubjectPage,
      deleteLanguage
    )
  }
}
