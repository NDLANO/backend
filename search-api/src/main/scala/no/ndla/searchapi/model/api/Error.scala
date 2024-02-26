/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.api

import no.ndla.common.Clock
import no.ndla.common.errors.AccessDeniedException
import no.ndla.network.tapir.{AllErrors, TapirErrorHelpers}
import no.ndla.search.{IndexNotFoundException, NdlaSearchException}
import no.ndla.searchapi.Props
import sttp.tapir.Schema.annotations.description

import java.time.LocalDateTime

@description("Information about an error")
case class Error(
    @description("Code stating the type of error") code: String,
    @description("Description of the error") description: String,
    @description("An optional id referring to the cover") id: Option[Long] = None,
    @description("When the error occured") occuredAt: LocalDateTime = LocalDateTime.now()
)

@description("Information about validation errors")
case class ValidationError(
    @description("Code stating the type of error") code: String,
    @description("Description of the error") description: String,
    @description("List of validation messages") messages: Seq[ValidationMessage],
    @description("When the error occured") occuredAt: LocalDateTime = LocalDateTime.now()
)

@description("A message describing a validation error on a specific field")
case class ValidationMessage(
    @description("The field the error occured in") field: String,
    @description("The validation message") message: String
)

trait ErrorHelpers extends TapirErrorHelpers {
  this: Props with Clock =>

  import ErrorHelpers._
  import SearchErrorHelpers._

  override def handleErrors: PartialFunction[Throwable, AllErrors] = {
    case rw: ResultWindowTooLargeException => errorBody(WINDOW_TOO_LARGE, rw.getMessage, 422)
    case _: IndexNotFoundException         => errorBody(INDEX_MISSING, INDEX_MISSING_DESCRIPTION, 503)
    case _: InvalidIndexBodyException      => errorBody(INVALID_BODY, INVALID_BODY_DESCRIPTION, 400)
    case te: TaxonomyException             => errorBody(TAXONOMY_FAILURE, te.getMessage, 500)
    case ade: AccessDeniedException        => forbiddenMsg(ade.getMessage)
    case NdlaSearchException(_, Some(rf), _)
        if rf.error.rootCause
          .exists(x => x.`type` == "search_context_missing_exception" || x.reason == "Cannot parse scroll id") =>
      invalidSearchContext
  }

  object SearchErrorHelpers {
    val GENERIC                = "GENERIC"
    val INVALID_BODY           = "INVALID_BODY"
    val TAXONOMY_FAILURE       = "TAXONOMY_FAILURE"
    val INVALID_SEARCH_CONTEXT = "INVALID_SEARCH_CONTEXT"
    val ACCESS_DENIED          = "ACCESS DENIED"

    val GENERIC_DESCRIPTION =
      s"Ooops. Something we didn't anticipate occured. We have logged the error, and will look into it. But feel free to contact ${props.ContactEmail} if the error persists."

    val WINDOW_TOO_LARGE_DESCRIPTION =
      s"The result window is too large. Fetching pages above ${props.ElasticSearchIndexMaxResultWindow} results requires scrolling, see query-parameter 'search-context'."

    val INVALID_BODY_DESCRIPTION =
      "Unable to index the requested document because body was invalid."

    val INVALID_SEARCH_CONTEXT_DESCRIPTION =
      "The search-context specified was not expected. Please create one by searching from page 1."

    val GenericError: Error         = Error(GENERIC, GENERIC_DESCRIPTION)
    val IndexMissingError: Error    = Error(INDEX_MISSING, INDEX_MISSING_DESCRIPTION)
    val InvalidBody: Error          = Error(INVALID_BODY, INVALID_BODY_DESCRIPTION)
    val InvalidSearchContext: Error = Error(INVALID_SEARCH_CONTEXT, INVALID_SEARCH_CONTEXT_DESCRIPTION)
  }
  case class ResultWindowTooLargeException(message: String = SearchErrorHelpers.WINDOW_TOO_LARGE_DESCRIPTION)
      extends RuntimeException(message)
  case class InvalidIndexBodyException(message: String = SearchErrorHelpers.INVALID_BODY_DESCRIPTION)
      extends RuntimeException(message)
}

class ValidationException(message: String = "Validation Error", val errors: Seq[ValidationMessage])
    extends RuntimeException(message)
class ApiSearchException(val apiName: String, message: String) extends RuntimeException(message)
case class ElasticIndexingException(message: String)           extends RuntimeException(message)
case class TaxonomyException(message: String)                  extends RuntimeException(message)
case class GrepException(message: String)                      extends RuntimeException(message)
