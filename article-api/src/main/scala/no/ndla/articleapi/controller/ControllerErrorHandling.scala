/*
 * Part of NDLA article-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import no.ndla.common.Clock
import no.ndla.common.errors.{AccessDeniedException, ValidationException}
import no.ndla.articleapi.model.api.NotFoundException
import no.ndla.database.DataSource
import no.ndla.network.tapir.{AllErrors, ErrorBody, ErrorHandling, ErrorHelpers, NotFoundWithSupportedLanguages}
import no.ndla.search.{IndexNotFoundException, NdlaSearchException}
import org.postgresql.util.PSQLException

class ControllerErrorHandling(using dataSource: => DataSource, errorHelpers: ErrorHelpers, clock: Clock)
    extends ErrorHandling {
  import errorHelpers.*

  override def handleErrors: PartialFunction[Throwable, AllErrors] = {
    case a: AccessDeniedException if a.unauthorized =>
      ErrorBody(ACCESS_DENIED, a.getMessage, clock.now(), 401)
    case a: AccessDeniedException =>
      ErrorBody(ACCESS_DENIED, a.getMessage, clock.now(), 403)
    case v: ValidationException                         => validationError(v)
    case _: IndexNotFoundException                      => errorBody(INDEX_MISSING, INDEX_MISSING_DESCRIPTION, 500)
    case NotFoundException(message, sl) if sl.isEmpty   => notFoundWithMsg(message)
    case NotFoundException(message, supportedLanguages) =>
      NotFoundWithSupportedLanguages(NOT_FOUND, message, clock.now(), supportedLanguages, 404)
    case rw: ArticleErrorHelpers.ResultWindowTooLargeException =>
      errorBody(WINDOW_TOO_LARGE, rw.getMessage, 422)
    case _: PSQLException =>
      dataSource.connectToDatabase()
      errorBody(DATABASE_UNAVAILABLE, DATABASE_UNAVAILABLE_DESCRIPTION, 500)
    case NdlaSearchException(_, Some(rf), _, _)
        if rf.error.rootCause
          .exists(x => x.`type` == "search_context_missing_exception" || x.reason == "Cannot parse scroll id") =>
      errorBody(INVALID_SEARCH_CONTEXT, INVALID_SEARCH_CONTEXT_DESCRIPTION, 400)
    case age: ArticleErrorHelpers.ArticleGoneException =>
      errorBody(ArticleErrorHelpers.ARTICLE_GONE, age.getMessage, 410)
  }
}

object ArticleErrorHelpers {
  val ARTICLE_GONE                         = "ARTICLE_GONE"
  val WINDOW_TOO_LARGE_DESCRIPTION: String =
    "The result window is too large. Fetching pages above the maximum result window requires scrolling, see query-parameter 'search-context'."
  val ARTICLE_GONE_DESCRIPTION = "The article you are searching for seems to have vanished 👻"

  case class ResultWindowTooLargeException(message: String = WINDOW_TOO_LARGE_DESCRIPTION)
      extends RuntimeException(message)
  case class ArticleGoneException(message: String = ARTICLE_GONE_DESCRIPTION) extends RuntimeException(message)
}
