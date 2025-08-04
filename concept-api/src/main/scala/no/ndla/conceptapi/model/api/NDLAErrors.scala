/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.model.api

import no.ndla.common.Clock
import no.ndla.common.errors.{AccessDeniedException, ValidationException, OperationNotAllowedException}
import no.ndla.conceptapi.Props
import no.ndla.database.DataSource
import no.ndla.network.model.HttpRequestException
import no.ndla.network.tapir.{AllErrors, TapirErrorHandling}
import no.ndla.search.{IndexNotFoundException, NdlaSearchException}
import org.postgresql.util.PSQLException

trait ErrorHandling extends TapirErrorHandling {
  this: Props & Clock & DataSource =>

  import ConceptErrorHelpers.*
  import ErrorHelpers.*

  override def handleErrors: PartialFunction[Throwable, AllErrors] = {
    case a: AccessDeniedException         => forbiddenMsg(a.getMessage)
    case v: ValidationException           => validationError(v)
    case n: NotFoundException             => notFoundWithMsg(n.getMessage)
    case o: OptimisticLockException       => errorBody(RESOURCE_OUTDATED, o.getMessage, 409)
    case st: IllegalStatusStateTransition => badRequest(st.getMessage)
    case _: IndexNotFoundException        => errorBody(INDEX_MISSING, INDEX_MISSING_DESCRIPTION, 500)
    case NdlaSearchException(_, Some(rf), _, _)
        if rf.error.rootCause
          .exists(x => x.`type` == "search_context_missing_exception" || x.reason == "Cannot parse scroll id") =>
      errorBody(INVALID_SEARCH_CONTEXT, INVALID_SEARCH_CONTEXT_DESCRIPTION, 400)
    case ona: OperationNotAllowedException => errorBody(OPERATION_NOT_ALLOWED, ona.getMessage, 400)
    case psqle: PSQLException              =>
      DataSource.connectToDatabase()
      logger.error("Something went wrong with database connections", psqle)
      errorBody(DATABASE_UNAVAILABLE, DATABASE_UNAVAILABLE_DESCRIPTION, 500)
    case h: HttpRequestException =>
      h.httpResponse match {
        case Some(resp) if resp.code.isClientError => errorBody(VALIDATION, resp.body, 400)
        case _                                     =>
          logger.error(s"Problem with remote service: ${h.getMessage}")
          errorBody(GENERIC, GENERIC_DESCRIPTION, 502)
      }
  }

  object ConceptErrorHelpers {
    val OPERATION_NOT_ALLOWED                = "OPERATION_NOT_ALLOWED"
    val WINDOW_TOO_LARGE_DESCRIPTION: String =
      s"The result window is too large. Fetching pages above ${props.ElasticSearchIndexMaxResultWindow} results requires scrolling, see query-parameter 'search-context'."

  }

  case class OptimisticLockException(message: String = ErrorHelpers.RESOURCE_OUTDATED_DESCRIPTION)
      extends RuntimeException(message)
  case class IllegalStatusStateTransition(message: String = ErrorHelpers.ILLEGAL_STATUS_TRANSITION)
      extends RuntimeException(message)
  class ResultWindowTooLargeException(message: String = ConceptErrorHelpers.WINDOW_TOO_LARGE_DESCRIPTION)
      extends RuntimeException(message)
}

case class NotFoundException(message: String, supportedLanguages: Seq[String] = Seq.empty)
    extends RuntimeException(message)
case class ConceptMissingIdException(message: String)     extends RuntimeException(message)
case class ConceptExistsAlreadyException(message: String) extends RuntimeException(message)
