/*
 * Part of NDLA audio-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.Props
import no.ndla.audioapi.model.api.{ImportException, JobAlreadyFoundException, OptimisticLockException}
import no.ndla.common.Clock
import no.ndla.common.errors.{AccessDeniedException, FileTooBigException, NotFoundException, ValidationException}
import no.ndla.database.DataSource
import no.ndla.network.model.HttpRequestException
import no.ndla.network.tapir.{AllErrors, ErrorBody, ErrorHandling, ErrorHelpers, ValidationErrorBody}
import org.postgresql.util.PSQLException
import no.ndla.search.NdlaSearchException

class ControllerErrorHandling(using
    props: Props,
    dataSource: => DataSource,
    errorHelpers: ErrorHelpers,
    clock: Clock
) extends ErrorHandling {
  import errorHelpers.*
  val fileTooBigDescription: String =
    s"The file is too big. Max file size is ${props.MaxAudioFileSizeBytes / 1024 / 1024} MiB"

  val WINDOW_TOO_LARGE_DESCRIPTION: String =
    s"The result window is too large. Fetching pages above ${props.ElasticSearchIndexMaxResultWindow} results requires scrolling, see query-parameter 'search-context'."

  class ResultWindowTooLargeException(message: String = WINDOW_TOO_LARGE_DESCRIPTION) extends RuntimeException(message)

  override def handleErrors: PartialFunction[Throwable, AllErrors] = {
    case a: AccessDeniedException => ErrorBody(ACCESS_DENIED, a.getMessage, clock.now(), 403)
    case v: ValidationException   =>
      ValidationErrorBody(VALIDATION, "Validation Error", clock.now(), Some(v.errors), 400)
    case hre: HttpRequestException         => ErrorBody(REMOTE_ERROR, hre.getMessage, clock.now(), 502)
    case rw: ResultWindowTooLargeException => ErrorBody(WINDOW_TOO_LARGE, rw.getMessage, clock.now(), 422)
    case i: ImportException                => ErrorBody(IMPORT_FAILED, i.getMessage, clock.now(), 422)
    case nfe: NotFoundException            => notFoundWithMsg(nfe.getMessage)
    case o: OptimisticLockException        => ErrorBody(RESOURCE_OUTDATED, o.getMessage, clock.now(), 409)
    case _: FileTooBigException            => ErrorBody(FILE_TOO_BIG, fileTooBigDescription, clock.now(), 413)
    case _: PSQLException                  =>
      dataSource.connectToDatabase()
      ErrorBody(DATABASE_UNAVAILABLE, DATABASE_UNAVAILABLE_DESCRIPTION, clock.now(), 500)
    case NdlaSearchException(_, Some(rf), _, _)
        if rf.error.rootCause
          .exists(x => x.`type` == "search_context_missing_exception" || x.reason == "Cannot parse scroll id") =>
      invalidSearchContext
    case jafe: JobAlreadyFoundException => ErrorBody(JOB_ALREADY_FOUND, jafe.getMessage, clock.now(), 400)
  }
}
