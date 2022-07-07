/*
 * Part of NDLA image-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.api

import no.ndla.common.DateParser

import java.time.LocalDateTime
import no.ndla.imageapi.Props
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Information about errors")
case class Error(
    @(ApiModelProperty @field)(description = "Code stating the type of error") code: String,
    @(ApiModelProperty @field)(description = "Description of the error") description: String,
    @(ApiModelProperty @field)(description = "When the error occurred") occurredAt: String =
      DateParser.dateToString(LocalDateTime.now(), withMillis = false)
)

trait ErrorHelpers {
  this: Props =>

  object ErrorHelpers {
    val GENERIC                = "GENERIC"
    val NOT_FOUND              = "NOT FOUND"
    val INDEX_MISSING          = "INDEX MISSING"
    val VALIDATION             = "VALIDATION"
    val FILE_TOO_BIG           = "FILE TOO BIG"
    val ACCESS_DENIED          = "ACCESS DENIED"
    val GATEWAY_TIMEOUT        = "GATEWAY TIMEOUT"
    val WINDOW_TOO_LARGE       = "RESULT WINDOW TOO LARGE"
    val IMPORT_FAILED          = "IMPORT FAILED"
    val DATABASE_UNAVAILABLE   = "DATABASE_UNAVAILABLE"
    val INVALID_SEARCH_CONTEXT = "INVALID_SEARCH_CONTEXT"
    val INVALID_URL            = "INVALID_URL"

    val GenericError: Error = Error(
      GENERIC,
      s"Ooops. Something we didn't anticipate occurred. We have logged the error, and will look into it. But feel free to contact ${props.ContactEmail} if the error persists."
    )

    val IndexMissingError: Error = Error(
      INDEX_MISSING,
      s"Ooops. Our search index is not available at the moment, but we are trying to recreate it. Please try again in a few minutes. Feel free to contact ${props.ContactEmail} if the error persists."
    )

    val FileTooBigError: Error = Error(
      FILE_TOO_BIG,
      s"The file is too big. Max file size is ${props.MaxImageFileSizeBytes / 1024 / 1024} MiB"
    )
    val ImageNotFoundError: Error = Error(NOT_FOUND, s"Ooops. That image does not exists")

    val WindowTooLargeError: Error = Error(
      WINDOW_TOO_LARGE,
      s"The result window is too large. Fetching pages above ${props.ElasticSearchIndexMaxResultWindow} results requires scrolling, see query-parameter 'search-context'."
    )
    val DatabaseUnavailableError: Error =
      Error(DATABASE_UNAVAILABLE, s"Database seems to be unavailable, retrying connection.")

    val InvalidSearchContext: Error = Error(
      INVALID_SEARCH_CONTEXT,
      "The search-context specified was not expected. Please create one by searching from page 1."
    )
  }
}
