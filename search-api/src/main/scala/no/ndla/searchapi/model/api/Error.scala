/*
 * Part of NDLA search-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.api

import cats.implicits.catsSyntaxOptionId
import no.ndla.common.Clock
import no.ndla.common.errors.{AccessDeniedException, ValidationException}
import no.ndla.network.tapir.{AllErrors, TapirErrorHelpers, ValidationErrorBody}
import no.ndla.search.{IndexNotFoundException, NdlaSearchException}
import no.ndla.searchapi.Props

trait ErrorHelpers extends TapirErrorHelpers {
  this: Props with Clock =>

  import ErrorHelpers._
  import SearchErrorHelpers._

  override def handleErrors: PartialFunction[Throwable, AllErrors] = {
    case rw: ResultWindowTooLargeException => errorBody(WINDOW_TOO_LARGE, rw.getMessage, 422)
    case _: IndexNotFoundException         => errorBody(INDEX_MISSING, INDEX_MISSING_DESCRIPTION, 503)
    case _: InvalidIndexBodyException      => errorBody(INVALID_BODY, INVALID_BODY_DESCRIPTION, 400)
    case te: TaxonomyException             => errorBody(TAXONOMY_FAILURE, te.getMessage, 500)
    case v: ValidationException =>
      ValidationErrorBody(VALIDATION, VALIDATION_DESCRIPTION, clock.now(), messages = v.errors.some, 400)
    case ade: AccessDeniedException => forbiddenMsg(ade.getMessage)
    case NdlaSearchException(_, Some(rf), _, _)
        if rf.error.rootCause
          .exists(x => x.`type` == "search_context_missing_exception" || x.reason == "Cannot parse scroll id") =>
      invalidSearchContext
  }

  object SearchErrorHelpers {
    val GENERIC          = "GENERIC"
    val INVALID_BODY     = "INVALID_BODY"
    val TAXONOMY_FAILURE = "TAXONOMY_FAILURE"

    val WINDOW_TOO_LARGE_DESCRIPTION: String =
      s"The result window is too large. Fetching pages above ${props.ElasticSearchIndexMaxResultWindow} results requires scrolling, see query-parameter 'search-context'."

    val INVALID_BODY_DESCRIPTION =
      "Unable to index the requested document because body was invalid."
  }
  case class ResultWindowTooLargeException(message: String = SearchErrorHelpers.WINDOW_TOO_LARGE_DESCRIPTION)
      extends RuntimeException(message)
  case class InvalidIndexBodyException(message: String = SearchErrorHelpers.INVALID_BODY_DESCRIPTION)
      extends RuntimeException(message)
}

class ApiSearchException(val apiName: String, message: String) extends RuntimeException(message)
case class ElasticIndexingException(message: String)           extends RuntimeException(message)
case class TaxonomyException(message: String)                  extends RuntimeException(message)
case class GrepException(message: String)                      extends RuntimeException(message)
