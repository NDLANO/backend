/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import no.ndla.audioapi.Props
import no.ndla.audioapi.integration.DataSource
import no.ndla.common.Clock
import no.ndla.common.errors.{AccessDeniedException, ValidationException}
import no.ndla.network.model.HttpRequestException
import no.ndla.network.tapir.{ErrorBody, TapirErrorHelpers}
import no.ndla.search.NdlaSearchException
import org.postgresql.util.PSQLException
import org.scalatra.servlet.SizeConstraintExceededException

trait ErrorHelpers extends TapirErrorHelpers {
  this: Props with Clock with DataSource =>

  import ErrorHelpers._

  object Helpers {
    val fileTooBigDescription =
      s"The file is too big. Max file size is ${props.MaxAudioFileSizeBytes / 1024 / 1024} MiB"

    val WINDOW_TOO_LARGE_DESCRIPTION =
      s"The result window is too large. Fetching pages above ${props.ElasticSearchIndexMaxResultWindow} results requires scrolling, see query-parameter 'search-context'."

    class ResultWindowTooLargeException(message: String = WINDOW_TOO_LARGE_DESCRIPTION)
        extends RuntimeException(message)

    class OptimisticLockException(message: String = ErrorHelpers.RESOURCE_OUTDATED_DESCRIPTION)
        extends RuntimeException(message)
  }

  import Helpers._

  override def returnError(ex: Throwable): ErrorBody = ex match {
    case a: AccessDeniedException => ErrorBody(ACCESS_DENIED, a.getMessage, clock.now(), 403)
    case v: ValidationException =>
      ErrorBody(VALIDATION, "Validation Error", clock.now(), Some(v.errors), 400)
    case hre: HttpRequestException          => ErrorBody(REMOTE_ERROR, hre.getMessage, clock.now(), 502)
    case rw: ResultWindowTooLargeException  => ErrorBody(WINDOW_TOO_LARGE, rw.getMessage, clock.now(), 422)
    case i: ImportException                 => ErrorBody(IMPORT_FAILED, i.getMessage, clock.now(), 422)
    case nfe: NotFoundException             => notFoundWithMsg(nfe.getMessage)
    case o: OptimisticLockException         => ErrorBody(RESOURCE_OUTDATED, o.getMessage, clock.now(), 409)
    case _: SizeConstraintExceededException => ErrorBody(FILE_TOO_BIG, fileTooBigDescription, clock.now(), 413)
    case _: PSQLException =>
      DataSource.connectToDatabase()
      ErrorBody(DATABASE_UNAVAILABLE, DATABASE_UNAVAILABLE_DESCRIPTION, clock.now(), 500)
    case NdlaSearchException(_, Some(rf), _)
        if rf.error.rootCause
          .exists(x => x.`type` == "search_context_missing_exception" || x.reason == "Cannot parse scroll id") =>
      invalidSearchContext
    case t: Throwable => {
      logger.error(t)(t.getMessage)
      generic
    }
  }

}

class NotFoundException(message: String = "The audio was not found") extends RuntimeException(message)
case class MissingIdException(message: String)                       extends RuntimeException(message)
case class CouldNotFindLanguageException(message: String)            extends RuntimeException(message)
class AudioStorageException(message: String)                         extends RuntimeException(message)
class LanguageMappingException(message: String)                      extends RuntimeException(message)
class ImportException(message: String)                               extends RuntimeException(message)
case class ElasticIndexingException(message: String)                 extends RuntimeException(message)
