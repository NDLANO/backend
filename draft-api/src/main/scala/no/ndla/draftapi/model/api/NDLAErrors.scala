/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import no.ndla.common.Clock
import no.ndla.common.errors.{AccessDeniedException, FileTooBigException, ValidationException}
import no.ndla.draftapi.Props
import no.ndla.draftapi.integration.DataSource
import no.ndla.network.model.HttpRequestException
import no.ndla.network.tapir.{AllErrors, ErrorBody, TapirErrorHelpers}
import no.ndla.search.{IndexNotFoundException, NdlaSearchException}
import org.postgresql.util.PSQLException
import sttp.tapir.Schema.annotations.description

import java.time.LocalDateTime

@description("Information about an error")
case class Error(
    @description("Code stating the type of error") code: String,
    @description("Description of the error") description: String,
    @description("When the error occured") occuredAt: LocalDateTime = LocalDateTime.now()
)

trait ErrorHelpers extends TapirErrorHelpers {
  this: Props with Clock with DataSource =>

  import ErrorHelpers._

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
    case _: FileTooBigException =>
      ErrorBody(
        FILE_TOO_BIG,
        DraftErrorHelpers.fileTooBigDescription,
        clock.now(),
        413
      )
    case psql: PSQLException =>
      logger.error(s"Got postgres exception: '${psql.getMessage}', attempting db reconnect", psql)
      DataSource.connectToDatabase()
      ErrorBody(DATABASE_UNAVAILABLE, DATABASE_UNAVAILABLE_DESCRIPTION, clock.now(), 500)
    case h: HttpRequestException =>
      h.httpResponse match {
        case Some(resp) if resp.code.isClientError =>
          ErrorBody(VALIDATION, resp.body, clock.now(), 400)
        case _ =>
          logger.error(s"Problem with remote service: ${h.getMessage}")
          ErrorBody(GENERIC, GENERIC_DESCRIPTION, clock.now(), 502)
      }
    case NdlaSearchException(_, Some(rf), _)
        if rf.error.rootCause
          .exists(x => x.`type` == "search_context_missing_exception" || x.reason == "Cannot parse scroll id") =>
      ErrorBody(INVALID_SEARCH_CONTEXT, INVALID_SEARCH_CONTEXT_DESCRIPTION, clock.now(), 400)
  }

  object DraftErrorHelpers {
    val WINDOW_TOO_LARGE_DESCRIPTION =
      s"The result window is too large. Fetching pages above ${props.ElasticSearchIndexMaxResultWindow} results requires scrolling, see query-parameter 'search-context'."

    val fileTooBigDescription =
      s"The file is too big. Max file size is ${props.multipartFileSizeThresholdBytes / 1024 / 1024} MiB"
  }

  class OptimisticLockException(message: String = RESOURCE_OUTDATED_DESCRIPTION)       extends RuntimeException(message)
  case class IllegalStatusStateTransition(message: String = ILLEGAL_STATUS_TRANSITION) extends RuntimeException(message)
  class ResultWindowTooLargeException(
      message: String = DraftErrorHelpers.WINDOW_TOO_LARGE_DESCRIPTION
  ) extends RuntimeException(message)

}

case class NotFoundException(message: String, supportedLanguages: Seq[String] = Seq.empty)
    extends RuntimeException(message)
case class ArticlePublishException(message: String)    extends RuntimeException(message)
case class ArticleVersioningException(message: String) extends RuntimeException(message)

class ArticleStatusException(message: String)            extends RuntimeException(message)
case class OperationNotAllowedException(message: String) extends RuntimeException(message)
case class CloneFileException(message: String)           extends RuntimeException(message)
case class H5PException(message: String)                 extends RuntimeException(message)
case class GenerateIDException(message: String)          extends RuntimeException(message)
