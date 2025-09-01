/*
 * Part of NDLA concept-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.controller

import no.ndla.common.Clock
import no.ndla.common.errors.{AccessDeniedException, FileTooBigException, NotFoundException, ValidationException}
import no.ndla.conceptapi.model.api.{
  NotFoundException => OldNotFoundException,
  OptimisticLockException,
  ResultWindowTooLargeException
}
import no.ndla.database.DataSource
import no.ndla.network.model.HttpRequestException
import no.ndla.network.tapir.{AllErrors, ErrorBody, ErrorHandling, ErrorHelpers, ValidationErrorBody}
import no.ndla.search.{IndexNotFoundException, NdlaSearchException}
import org.postgresql.util.PSQLException

class ControllerErrorHandling(using
    dataSource: => DataSource,
    errorHelpers: ErrorHelpers,
    clock: Clock
) extends ErrorHandling {
  import errorHelpers.*

  override def handleErrors: PartialFunction[Throwable, AllErrors] = {
    case a: AccessDeniedException => ErrorBody(ACCESS_DENIED, a.getMessage, clock.now(), 403)
    case v: ValidationException   =>
      ValidationErrorBody(VALIDATION, "Validation Error", clock.now(), Some(v.errors), 400)
    case hre: HttpRequestException         => ErrorBody(REMOTE_ERROR, hre.getMessage, clock.now(), 502)
    case rw: ResultWindowTooLargeException => ErrorBody(WINDOW_TOO_LARGE, rw.getMessage, clock.now(), 422)
    case nfe: OldNotFoundException         => ErrorBody(NOT_FOUND, nfe.getMessage, clock.now(), 404)
    case nfe: NotFoundException            => ErrorBody(NOT_FOUND, nfe.getMessage, clock.now(), 404)
    case o: OptimisticLockException        => ErrorBody(RESOURCE_OUTDATED, o.getMessage, clock.now(), 409)
    case _: FileTooBigException            => ErrorBody(FILE_TOO_BIG, "File too big", clock.now(), 413)
    case _: PSQLException                  =>
      dataSource.connectToDatabase()
      ErrorBody(DATABASE_UNAVAILABLE, DATABASE_UNAVAILABLE_DESCRIPTION, clock.now(), 500)
    case _: IndexNotFoundException =>
      ErrorBody(INDEX_MISSING, INDEX_MISSING_DESCRIPTION, clock.now(), 500)
    case e: NdlaSearchException[?] =>
      logger.error(e.getMessage)
      ErrorBody(GENERIC, e.getMessage, clock.now(), 500)
  }
}
