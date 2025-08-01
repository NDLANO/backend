/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import no.ndla.audioapi.Props
import no.ndla.common.Clock
import no.ndla.common.errors.{AccessDeniedException, FileTooBigException, NotFoundException, ValidationException}
import no.ndla.database.DataSource
import no.ndla.network.model.HttpRequestException
import no.ndla.network.tapir.{AllErrors, ErrorBody, TapirErrorHandling, ValidationErrorBody}
import no.ndla.search.NdlaSearchException
import org.postgresql.util.PSQLException

trait ErrorHandling extends TapirErrorHandling {
  this: Props with Clock with DataSource =>

  import ErrorHelpers._

  object Helpers {
    val fileTooBigDescription: String =
      s"The file is too big. Max file size is ${props.MaxAudioFileSizeBytes / 1024 / 1024} MiB"

    val WINDOW_TOO_LARGE_DESCRIPTION: String =
      s"The result window is too large. Fetching pages above ${props.ElasticSearchIndexMaxResultWindow} results requires scrolling, see query-parameter 'search-context'."

    class ResultWindowTooLargeException(message: String = WINDOW_TOO_LARGE_DESCRIPTION)
        extends RuntimeException(message)

    class OptimisticLockException(message: String = ErrorHelpers.RESOURCE_OUTDATED_DESCRIPTION)
        extends RuntimeException(message)
  }

  import Helpers._

  override def handleErrors: PartialFunction[Throwable, AllErrors] = {
    case a: AccessDeniedException => ErrorBody(ACCESS_DENIED, a.getMessage, clock.now(), 403)
    case v: ValidationException   =>
      new ValidationErrorBody(VALIDATION, "Validation Error", clock.now(), Some(v.errors), 400)
    case hre: HttpRequestException         => ErrorBody(REMOTE_ERROR, hre.getMessage, clock.now(), 502)
    case rw: ResultWindowTooLargeException => ErrorBody(WINDOW_TOO_LARGE, rw.getMessage, clock.now(), 422)
    case i: ImportException                => ErrorBody(IMPORT_FAILED, i.getMessage, clock.now(), 422)
    case nfe: NotFoundException            => notFoundWithMsg(nfe.getMessage)
    case o: OptimisticLockException        => ErrorBody(RESOURCE_OUTDATED, o.getMessage, clock.now(), 409)
    case _: FileTooBigException            => ErrorBody(FILE_TOO_BIG, fileTooBigDescription, clock.now(), 413)
    case _: PSQLException                  =>
      DataSource.connectToDatabase()
      ErrorBody(DATABASE_UNAVAILABLE, DATABASE_UNAVAILABLE_DESCRIPTION, clock.now(), 500)
    case NdlaSearchException(_, Some(rf), _, _)
        if rf.error.rootCause
          .exists(x => x.`type` == "search_context_missing_exception" || x.reason == "Cannot parse scroll id") =>
      invalidSearchContext
    case jafe: JobAlreadyFoundException => ErrorBody(JOB_ALREADY_FOUND, jafe.getMessage, clock.now(), 400)
  }

}

case class CouldNotFindLanguageException(message: String) extends RuntimeException(message)
class AudioStorageException(message: String)              extends RuntimeException(message)
class LanguageMappingException(message: String)           extends RuntimeException(message)
class ImportException(message: String)                    extends RuntimeException(message)
case class JobAlreadyFoundException(message: String)      extends RuntimeException(message)
