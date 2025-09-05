/*
 * Part of NDLA draft-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.controller

import no.ndla.common.Clock
import no.ndla.common.errors.{
  AccessDeniedException,
  FileTooBigException,
  OperationNotAllowedException,
  ValidationException
}
import no.ndla.draftapi.model.api.{
  ArticlePublishException,
  ArticleStatusException,
  DraftErrorHelpers,
  IllegalStatusStateTransition,
  NotFoundException
}
import no.ndla.database.DataSource
import no.ndla.network.tapir.{AllErrors, ErrorBody, ErrorHandling, ErrorHelpers}
import no.ndla.search.{IndexNotFoundException, NdlaSearchException}
import no.ndla.network.model.HttpRequestException
import org.postgresql.util.PSQLException

class ControllerErrorHandling(using
    dataSource: => DataSource,
    errorHelpers: ErrorHelpers,
    draftErrorHelpers: DraftErrorHelpers,
    clock: => Clock
) extends ErrorHandling {
  import errorHelpers.*
  import draftErrorHelpers.*

  override def handleErrors: PartialFunction[Throwable, AllErrors] = {
    case a: AccessDeniedException if a.unauthorized => ErrorBody(ACCESS_DENIED, a.getMessage, clock.now(), 401)
    case v: ValidationException                     => validationError(v)
    case as: ArticleStatusException                 => ErrorBody(VALIDATION, as.getMessage, clock.now(), 400)
    case _: IndexNotFoundException         => ErrorBody(INDEX_MISSING, INDEX_MISSING_DESCRIPTION, clock.now(), 500)
    case n: NotFoundException              => ErrorBody(NOT_FOUND, n.getMessage, clock.now(), 404)
    case o: OptimisticLockException        => ErrorBody(RESOURCE_OUTDATED, o.getMessage, clock.now(), 409)
    case rw: ResultWindowTooLargeException => ErrorBody(WINDOW_TOO_LARGE, rw.getMessage, clock.now(), 422)
    case pf: ArticlePublishException       => ErrorBody(PUBLISH, pf.getMessage, clock.now(), 400)
    case st: IllegalStatusStateTransition  => ErrorBody(VALIDATION, st.getMessage, clock.now(), 400)
    case ona: OperationNotAllowedException => ErrorBody(UNPROCESSABLE_ENTITY, ona.getMessage, clock.now(), 422)
    case _: FileTooBigException            => ErrorBody(FILE_TOO_BIG, fileTooBigDescription, clock.now(), 413)
    case psql: PSQLException               =>
      logger.error(s"Got postgres exception: '${psql.getMessage}', attempting db reconnect", psql)
      dataSource.connectToDatabase()
      ErrorBody(DATABASE_UNAVAILABLE, DATABASE_UNAVAILABLE_DESCRIPTION, clock.now(), 500)
    case h: HttpRequestException =>
      h.httpResponse match {
        case Some(resp) if resp.code.isClientError =>
          ErrorBody(VALIDATION, resp.body, clock.now(), 400)
        case _ =>
          logger.error(s"Problem with remote service: ${h.getMessage}")
          ErrorBody(GENERIC, GENERIC_DESCRIPTION, clock.now(), 502)
      }
    case NdlaSearchException(_, Some(rf), _, _)
        if rf.error.rootCause
          .exists(x => x.`type` == "search_context_missing_exception" || x.reason == "Cannot parse scroll id") =>
      ErrorBody(INVALID_SEARCH_CONTEXT, INVALID_SEARCH_CONTEXT_DESCRIPTION, clock.now(), 400)
  }
}
