/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import java.util.Date

import no.ndla.audioapi.AudioApiProperties
import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Information about an error")
case class Error(
    @(ApiModelProperty @field)(description = "Code stating the type of error") code: String = Error.GENERIC,
    @(ApiModelProperty @field)(description = "Description of the error") description: String = Error.GENERIC_DESCRIPTION,
    @(ApiModelProperty @field)(description = "When the error occured") occuredAt: Date = new Date())

object Error {
  val GENERIC = "GENERIC"
  val NOT_FOUND = "NOT_FOUND"
  val INDEX_MISSING = "INDEX_MISSING"
  val REMOTE_ERROR = "REMOTE_ERROR"
  val VALIDATION = "VALIDATION_ERROR"
  val FILE_TOO_BIG = "FILE TOO BIG"
  val ACCESS_DENIED = "ACCESS DENIED"
  val WINDOW_TOO_LARGE = "RESULT_WINDOW_TOO_LARGE"
  val IMPORT_FAILED = "IMPORT_FAILED"
  val DATABASE_UNAVAILABLE = "DATABASE_UNAVAILABLE"
  val INVALID_SEARCH_CONTEXT = "INVALID_SEARCH_CONTEXT"

  val RESOURCE_OUTDATED_DESCRIPTION = "The resource is outdated. Please try fetching before submitting again."

  val GENERIC_DESCRIPTION =
    s"Ooops. Something we didn't anticipate occured. We have logged the error, and will look into it. But feel free to contact ${AudioApiProperties.ContactEmail} if the error persists."

  val FileTooBigError = Error(
    FILE_TOO_BIG,
    s"The file is too big. Max file size is ${AudioApiProperties.MaxAudioFileSizeBytes / 1024 / 1024} MiB")

  val WINDOW_TOO_LARGE_DESCRIPTION =
    s"The result window is too large. Fetching pages above ${AudioApiProperties.ElasticSearchIndexMaxResultWindow} results requires scrolling, see query-parameter 'search-context'."
  val DATABASE_UNAVAILABLE_DESCRIPTION = s"Database seems to be unavailable, retrying connection."

  val InvalidSearchContext = Error(
    INVALID_SEARCH_CONTEXT,
    "The search-context specified was not expected. Please create one by searching from page 1.")
}

class NotFoundException(message: String = "The audio was not found") extends RuntimeException(message)
case class MissingIdException(message: String) extends RuntimeException(message)

class ValidationException(message: String = "Validation error", val errors: Seq[ValidationMessage])
    extends RuntimeException(message)
class AccessDeniedException(message: String) extends RuntimeException(message)
class OptimisticLockException(message: String = Error.RESOURCE_OUTDATED_DESCRIPTION) extends RuntimeException(message)
case class ValidationMessage(field: String, message: String)
class AudioStorageException(message: String) extends RuntimeException(message)
class LanguageMappingException(message: String) extends RuntimeException(message)

class ResultWindowTooLargeException(message: String = Error.WINDOW_TOO_LARGE_DESCRIPTION)
    extends RuntimeException(message)
class ImportException(message: String) extends RuntimeException(message)
case class ElasticIndexingException(message: String) extends RuntimeException(message)
