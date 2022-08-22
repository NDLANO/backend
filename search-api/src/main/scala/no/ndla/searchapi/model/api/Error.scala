/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.api

import no.ndla.searchapi.Props

import java.time.LocalDateTime
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Information about an error")
case class Error(
    @(ApiModelProperty @field)(description = "Code stating the type of error") code: String,
    @(ApiModelProperty @field)(description = "Description of the error") description: String,
    @(ApiModelProperty @field)(description = "An optional id referring to the cover") id: Option[Long] = None,
    @(ApiModelProperty @field)(description = "When the error occured") occuredAt: LocalDateTime = LocalDateTime.now()
)

@ApiModel(description = "Information about validation errors")
case class ValidationError(
    @(ApiModelProperty @field)(description = "Code stating the type of error") code: String,
    @(ApiModelProperty @field)(description = "Description of the error") description: String,
    @(ApiModelProperty @field)(description = "List of validation messages") messages: Seq[ValidationMessage],
    @(ApiModelProperty @field)(description = "When the error occured") occuredAt: LocalDateTime = LocalDateTime.now()
)

@ApiModel(description = "A message describing a validation error on a specific field")
case class ValidationMessage(
    @(ApiModelProperty @field)(description = "The field the error occured in") field: String,
    @(ApiModelProperty @field)(description = "The validation message") message: String
)

trait ErrorHelpers {
  this: Props =>

  object ErrorHelpers {
    val GENERIC                = "GENERIC"
    val VALIDATION             = "VALIDATION"
    val WINDOW_TOO_LARGE       = "RESULT_WINDOW_TOO_LARGE"
    val INDEX_MISSING          = "INDEX_MISSING"
    val INVALID_BODY           = "INVALID_BODY"
    val TAXONOMY_FAILURE       = "TAXONOMY_FAILURE"
    val INVALID_SEARCH_CONTEXT = "INVALID_SEARCH_CONTEXT"
    val ACCESS_DENIED          = "ACCESS DENIED"

    val GENERIC_DESCRIPTION =
      s"Ooops. Something we didn't anticipate occured. We have logged the error, and will look into it. But feel free to contact ${props.ContactEmail} if the error persists."
    val VALIDATION_DESCRIPTION = "Validation Error"

    val WINDOW_TOO_LARGE_DESCRIPTION =
      s"The result window is too large. Fetching pages above ${props.ElasticSearchIndexMaxResultWindow} results requires scrolling, see query-parameter 'search-context'."

    val INDEX_MISSING_DESCRIPTION =
      s"Ooops. Our search index is not available at the moment, but we are trying to recreate it. Please try again in a few minutes. Feel free to contact ${props.ContactEmail} if the error persists."

    val INVALID_BODY_DESCRIPTION =
      "Unable to index the requested document because body was invalid."

    val INVALID_SEARCH_CONTEXT_DESCRIPTION =
      "The search-context specified was not expected. Please create one by searching from page 1."

    val GenericError: Error         = Error(GENERIC, GENERIC_DESCRIPTION)
    val IndexMissingError: Error    = Error(INDEX_MISSING, INDEX_MISSING_DESCRIPTION)
    val InvalidBody: Error          = Error(INVALID_BODY, INVALID_BODY_DESCRIPTION)
    val InvalidSearchContext: Error = Error(INVALID_SEARCH_CONTEXT, INVALID_SEARCH_CONTEXT_DESCRIPTION)
  }
  case class ResultWindowTooLargeException(message: String = ErrorHelpers.WINDOW_TOO_LARGE_DESCRIPTION)
      extends RuntimeException(message)
  case class InvalidIndexBodyException(message: String = ErrorHelpers.INVALID_BODY_DESCRIPTION)
      extends RuntimeException(message)
}

class ValidationException(message: String = "Validation Error", val errors: Seq[ValidationMessage])
    extends RuntimeException(message)
class ApiSearchException(val apiName: String, message: String) extends RuntimeException(message)
case class ElasticIndexingException(message: String)           extends RuntimeException(message)
case class TaxonomyException(message: String)                  extends RuntimeException(message)
case class GrepException(message: String)                      extends RuntimeException(message)
