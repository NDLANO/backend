/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import no.ndla.common.Clock
import no.ndla.common.errors.{AccessDeniedException, NotFoundException, ValidationException}
import no.ndla.database.DataSource
import no.ndla.learningpathapi.Props
import no.ndla.learningpathapi.model.domain.{ImportException, InvalidLpStatusException, OptimisticLockException}
import no.ndla.network.model.HttpRequestException
import no.ndla.network.tapir.{AllErrors, TapirErrorHandling}
import no.ndla.search.model.domain.ElasticIndexingException
import no.ndla.search.{IndexNotFoundException, NdlaSearchException}
import org.postgresql.util.PSQLException
import no.ndla.common.errors.OperationNotAllowedException

trait ErrorHandling extends TapirErrorHandling {
  this: Props with Clock with DataSource =>

  import ErrorHelpers._
  import LearningpathHelpers._
  override def handleErrors: PartialFunction[Throwable, AllErrors] = {
    case v: ValidationException =>
      validationError(v)
    case a: AccessDeniedException =>
      forbiddenMsg(a.getMessage)
    case _: OptimisticLockException =>
      errorBody(RESOURCE_OUTDATED, RESOURCE_OUTDATED_DESCRIPTION, 409)
    case nfe: NotFoundException =>
      notFoundWithMsg(nfe.getMessage)
    case hre: HttpRequestException =>
      errorBody(REMOTE_ERROR, hre.getMessage, 502)
    case i: ImportException =>
      errorBody(IMPORT_FAILED, i.getMessage, 422)
    case rw: ResultWindowTooLargeException =>
      errorBody(WINDOW_TOO_LARGE, rw.getMessage, 413)
    case _: IndexNotFoundException =>
      errorBody(INDEX_MISSING, INDEX_MISSING_DESCRIPTION, 500)
    case i: ElasticIndexingException =>
      errorBody(GENERIC, i.getMessage, 500)
    case _: PSQLException =>
      DataSource.connectToDatabase()
      errorBody(DATABASE_UNAVAILABLE, DATABASE_UNAVAILABLE_DESCRIPTION, 500)
    case mse: InvalidLpStatusException =>
      errorBody(MISSING_STATUS, mse.getMessage, 400)
    case one: OperationNotAllowedException => errorBody(UNPROCESSABLE_ENTITY, one.getMessage, 422)
    case NdlaSearchException(_, Some(rf), _, _)
        if rf.error.rootCause
          .exists(x => x.`type` == "search_context_missing_exception" || x.reason == "Cannot parse scroll id") =>
      errorBody(INVALID_SEARCH_CONTEXT, INVALID_SEARCH_CONTEXT_DESCRIPTION, 400)
  }

  object LearningpathHelpers {
    val WINDOW_TOO_LARGE_DESCRIPTION: String =
      s"The result window is too large. Fetching pages above ${props.ElasticSearchIndexMaxResultWindow} results requires scrolling, see query-parameter 'search-context'."
    case class ResultWindowTooLargeException(message: String = LearningpathHelpers.WINDOW_TOO_LARGE_DESCRIPTION)
        extends RuntimeException(message)
  }
}
