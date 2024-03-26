/*
 * Part of NDLA article-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import cats.implicits.catsSyntaxOptionId
import com.typesafe.scalalogging.StrictLogging
import no.ndla.articleapi.Props
import no.ndla.articleapi.integration.DataSource
import no.ndla.common.Clock
import no.ndla.common.errors.{AccessDeniedException, ValidationException}
import no.ndla.network.tapir.{
  AllErrors,
  ErrorBody,
  NotFoundWithSupportedLanguages,
  TapirErrorHelpers,
  ValidationErrorBody
}
import no.ndla.search.{IndexNotFoundException, NdlaSearchException}
import org.postgresql.util.PSQLException

trait ErrorHelpers extends TapirErrorHelpers with StrictLogging {
  this: Props with Clock with DataSource =>

  import ErrorHelpers._

  override def handleErrors: PartialFunction[Throwable, AllErrors] = {
    case a: AccessDeniedException if a.unauthorized =>
      ErrorBody(ACCESS_DENIED, a.getMessage, clock.now(), 401)
    case a: AccessDeniedException =>
      ErrorBody(ACCESS_DENIED, a.getMessage, clock.now(), 403)
    case v: ValidationException =>
      ValidationErrorBody(VALIDATION, VALIDATION_DESCRIPTION, clock.now(), messages = v.errors.some, 400)
    case _: IndexNotFoundException =>
      ErrorBody(INDEX_MISSING, INDEX_MISSING, clock.now(), 500)
    case NotFoundException(message, sl) if sl.isEmpty => notFoundWithMsg(message)
    case NotFoundException(message, supportedLanguages) =>
      NotFoundWithSupportedLanguages(NOT_FOUND, message, clock.now(), supportedLanguages, 404)
    case rw: ArticleErrorHelpers.ResultWindowTooLargeException =>
      ErrorBody(WINDOW_TOO_LARGE, rw.getMessage, clock.now(), 422)
    case _: PSQLException =>
      DataSource.connectToDatabase()
      ErrorBody(DATABASE_UNAVAILABLE, DATABASE_UNAVAILABLE_DESCRIPTION, clock.now(), 500)
    case NdlaSearchException(_, Some(rf), _)
        if rf.error.rootCause
          .exists(x => x.`type` == "search_context_missing_exception" || x.reason == "Cannot parse scroll id") =>
      ErrorBody(INVALID_SEARCH_CONTEXT, INVALID_SEARCH_CONTEXT_DESCRIPTION, clock.now(), 400)
    case age: ArticleErrorHelpers.ArticleGoneException =>
      ErrorBody(ArticleErrorHelpers.ARTICLE_GONE, age.getMessage, clock.now(), 410)
  }

  object ArticleErrorHelpers {
    val ARTICLE_GONE = "ARTICLE_GONE"

    val WINDOW_TOO_LARGE_DESCRIPTION: String =
      s"The result window is too large. Fetching pages above ${props.ElasticSearchIndexMaxResultWindow} results requires scrolling, see query-parameter 'search-context'."

    val ARTICLE_GONE_DESCRIPTION = "The article you are searching for seems to have vanished 👻"

    case class ResultWindowTooLargeException(message: String = WINDOW_TOO_LARGE_DESCRIPTION)
        extends RuntimeException(message)
    case class ArticleGoneException(message: String = ARTICLE_GONE_DESCRIPTION) extends RuntimeException(message)
  }

}

case class NotFoundException(message: String, supportedLanguages: Seq[String] = Seq.empty)
    extends RuntimeException(message)
case class ImportException(message: String)                             extends RuntimeException(message)
class ImportExceptions(val message: String, val errors: Seq[Throwable]) extends RuntimeException(message)
class ConfigurationException(message: String)                           extends RuntimeException(message)
