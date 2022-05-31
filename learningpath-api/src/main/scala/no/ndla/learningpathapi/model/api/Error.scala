/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import no.ndla.learningpathapi.Props
import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty
import java.util.Date
import scala.annotation.meta.field

@ApiModel(description = "Information about an error")
case class Error(
    @(ApiModelProperty @field)(description = "Code stating the type of error") code: String,
    @(ApiModelProperty @field)(description = "Description of the error") description: String,
    @(ApiModelProperty @field)(description = "When the error occured") occuredAt: Date = new Date()
)

trait ErrorHelpers {
  this: Props =>
  object ErrorHelpers {
    val GENERIC                = "GENERIC"
    val NOT_FOUND              = "NOT_FOUND"
    val INDEX_MISSING          = "INDEX_MISSING"
    val HEADER_MISSING         = "HEADER_MISSING"
    val VALIDATION             = "VALIDATION"
    val ACCESS_DENIED          = "ACCESS_DENIED"
    val REMOTE_ERROR           = "REMOTE_ERROR"
    val RESOURCE_OUTDATED      = "RESOURCE_OUTDATED"
    val WINDOW_TOO_LARGE       = "RESULT WINDOW TOO LARGE"
    val IMPORT_FAILED          = "IMPORT_FAILED"
    val DATABASE_UNAVAILABLE   = "DATABASE_UNAVAILABLE"
    val MISSING_STATUS         = "INVALID_STATUS"
    val INVALID_SEARCH_CONTEXT = "INVALID_SEARCH_CONTEXT"
    val DELETE_FAVORITE        = "DELETE_FAVORITE"

    val GENERIC_DESCRIPTION =
      s"Ooops. Something we didn't anticipate occured. We have logged the error, and will look into it. But feel free to contact ${props.ContactEmail} if the error persists."
    val VALIDATION_DESCRIPTION = "Validation Error"

    val RESOURCE_OUTDATED_DESCRIPTION =
      "The resource is outdated. Please try fetching before submitting again."

    val INDEX_MISSING_DESCRIPTION =
      s"Ooops. Our search index is not available at the moment, but we are trying to recreate it. Please try again in a few minutes. Feel free to contact ${props.ContactEmail} if the error persists."

    val WindowTooLargeError = Error(
      WINDOW_TOO_LARGE,
      s"The result window is too large. Fetching pages above ${props.ElasticSearchIndexMaxResultWindow} results requires scrolling, see query-parameter 'search-context'."
    )
    val IndexMissingError = Error(INDEX_MISSING, INDEX_MISSING_DESCRIPTION)
    val DatabaseUnavailableError =
      Error(DATABASE_UNAVAILABLE, s"Database seems to be unavailable, retrying connection.")
    val MISSING_STATUS_ERROR = "Parameter was not a valid status."

    val InvalidSearchContext = Error(
      INVALID_SEARCH_CONTEXT,
      "The search-context specified was not expected. Please create one by searching from page 1."
    )
  }
}
