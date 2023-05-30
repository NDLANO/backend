/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import cats.effect.IO
import no.ndla.audioapi.Props
import no.ndla.audioapi.integration.DataSource
import no.ndla.network.model.RequestInfo.ioLoggerContext
import no.ndla.common.Clock
import no.ndla.common.errors.{AccessDeniedException, ValidationException}
import no.ndla.common.logging.FLogging
import no.ndla.network.model.HttpRequestException
import no.ndla.network.tapir.{ErrorBody, TapirErrorHelpers}
import no.ndla.search.NdlaSearchException
import org.postgresql.util.PSQLException
import org.scalatra.servlet.SizeConstraintExceededException

trait ErrorHelpers extends TapirErrorHelpers with FLogging {
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

  override def returnError(ex: Throwable): IO[ErrorBody] =
    ex match {
      case a: AccessDeniedException => IO(ErrorBody(ACCESS_DENIED, a.getMessage, clock.now(), 403))
      case v: ValidationException =>
        IO(ErrorBody(VALIDATION, "Validation Error", clock.now(), Some(v.errors), 400))
      case hre: HttpRequestException          => IO(ErrorBody(REMOTE_ERROR, hre.getMessage, clock.now(), 502))
      case rw: ResultWindowTooLargeException  => IO(ErrorBody(WINDOW_TOO_LARGE, rw.getMessage, clock.now(), 422))
      case i: ImportException                 => IO(ErrorBody(IMPORT_FAILED, i.getMessage, clock.now(), 422))
      case nfe: NotFoundException             => IO(notFoundWithMsg(nfe.getMessage))
      case o: OptimisticLockException         => IO(ErrorBody(RESOURCE_OUTDATED, o.getMessage, clock.now(), 409))
      case _: SizeConstraintExceededException => IO(ErrorBody(FILE_TOO_BIG, fileTooBigDescription, clock.now(), 413))
      case _: PSQLException =>
        DataSource.connectToDatabase()
        IO(ErrorBody(DATABASE_UNAVAILABLE, DATABASE_UNAVAILABLE_DESCRIPTION, clock.now(), 500))
      case NdlaSearchException(_, Some(rf), _)
          if rf.error.rootCause
            .exists(x => x.`type` == "search_context_missing_exception" || x.reason == "Cannot parse scroll id") =>
        IO(invalidSearchContext)
      case t: Throwable =>
        logger.error(t)(t.getMessage).as(generic)
    }

}

class NotFoundException(message: String = "The audio was not found") extends RuntimeException(message)
case class MissingIdException(message: String)                       extends RuntimeException(message)
case class CouldNotFindLanguageException(message: String)            extends RuntimeException(message)
class AudioStorageException(message: String)                         extends RuntimeException(message)
class LanguageMappingException(message: String)                      extends RuntimeException(message)
class ImportException(message: String)                               extends RuntimeException(message)
case class ElasticIndexingException(message: String)                 extends RuntimeException(message)
