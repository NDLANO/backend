/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api

import java.util.Date
import scala.annotation.meta.field

import no.ndla.conceptapi.ConceptApiProperties
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(description = "Information about an error")
case class Error(
    @(ApiModelProperty @field)(description = "Code stating the type of error") code: String = Error.GENERIC,
    @(ApiModelProperty @field)(description = "Description of the error") description: String = Error.GENERIC_DESCRIPTION,
    @(ApiModelProperty @field)(description = "When the error occured") occuredAt: Date = new Date())

object Error {
  val GENERIC = "GENERIC"
  val NOT_FOUND = "NOT_FOUND"
  val INDEX_MISSING = "INDEX_MISSING"
  val VALIDATION = "VALIDATION"
  val RESOURCE_OUTDATED = "RESOURCE_OUTDATED"
  val ACCESS_DENIED = "ACCESS DENIED"
  val WINDOW_TOO_LARGE = "RESULT_WINDOW_TOO_LARGE"
  val PUBLISH = "PUBLISH"
  val DATABASE_UNAVAILABLE = "DATABASE_UNAVAILABLE"
  val INVALID_SEARCH_CONTEXT = "INVALID_SEARCH_CONTEXT"
  val OPERATION_NOT_ALLOWED = "OPERATION_NOT_ALLOWED"

  val VALIDATION_DESCRIPTION = "Validation Error"

  val GENERIC_DESCRIPTION =
    s"Ooops. Something we didn't anticipate occured. We have logged the error, and will look into it. But feel free to contact ${ConceptApiProperties.ContactEmail} if the error persists."

  val INDEX_MISSING_DESCRIPTION =
    s"Ooops. Our search index is not available at the moment, but we are trying to recreate it. Please try again in a few minutes. Feel free to contact ${ConceptApiProperties.ContactEmail} if the error persists."

  val RESOURCE_OUTDATED_DESCRIPTION =
    "The resource is outdated. Please try fetching before submitting again."

  val WINDOW_TOO_LARGE_DESCRIPTION =
    s"The result window is too large. Fetching pages above ${ConceptApiProperties.ElasticSearchIndexMaxResultWindow} results requires scrolling, see query-parameter 'search-context'."

  val DATABASE_UNAVAILABLE_DESCRIPTION =
    s"Database seems to be unavailable, retrying connection."

  val INVALID_SEARCH_CONTEXT_DESCRIPTION =
    "The search-context specified was not expected. Please create one by searching from page 1."

  val ILLEGAL_STATUS_TRANSITION: String = "Illegal status transition"

  val GenericError = Error(GENERIC, GENERIC_DESCRIPTION)
  val IndexMissingError = Error(INDEX_MISSING, INDEX_MISSING_DESCRIPTION)

  val InvalidSearchContext =
    Error(INVALID_SEARCH_CONTEXT, INVALID_SEARCH_CONTEXT_DESCRIPTION)

}

case class NotFoundException(message: String, supportedLanguages: Seq[String] = Seq.empty)
    extends RuntimeException(message)
case class ConceptMissingIdException(message: String) extends RuntimeException(message)
case class ConceptExistsAlreadyException(message: String) extends RuntimeException(message)
case class ImportException(message: String) extends RuntimeException(message)
case class ElasticIndexingException(message: String) extends RuntimeException(message)

class AccessDeniedException(message: String) extends RuntimeException(message)
case class OperationNotAllowedException(message: String) extends RuntimeException(message)
case class OptimisticLockException(message: String = Error.RESOURCE_OUTDATED_DESCRIPTION)
    extends RuntimeException(message)
case class IllegalStatusStateTransition(message: String = Error.ILLEGAL_STATUS_TRANSITION)
    extends RuntimeException(message)

class ResultWindowTooLargeException(message: String = Error.WINDOW_TOO_LARGE_DESCRIPTION)
    extends RuntimeException(message)
