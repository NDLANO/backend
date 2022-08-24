/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import no.ndla.articleapi.Props
import no.ndla.articleapi.integration.DataSource
import no.ndla.articleapi.model.api.{Error, ErrorHelpers, NotFoundException, ValidationError}
import no.ndla.common.errors.{AccessDeniedException, ValidationException}
import no.ndla.common.scalatra.NdlaControllerBase
import no.ndla.network.{ApplicationUrl, AuthUser, CorrelationID}
import no.ndla.search.{IndexNotFoundException, NdlaSearchException}
import org.apache.logging.log4j.ThreadContext
import org.json4s.ext.JavaTimeSerializers
import org.json4s.{DefaultFormats, Formats}
import org.postgresql.util.PSQLException
import org.scalatra._

trait NdlaController {
  this: Props with ErrorHelpers with DataSource =>

  import props._
  import ErrorHelpers._

  abstract class NdlaController extends NdlaControllerBase {
    protected implicit override val jsonFormats: Formats = DefaultFormats.withLong ++ JavaTimeSerializers.all

    before() {
      contentType = formats("json")
      CorrelationID.set(Option(request.getHeader(CorrelationIdHeader)))
      ThreadContext.put(CorrelationIdKey, CorrelationID.get.getOrElse(""))
      ApplicationUrl.set(request)
      AuthUser.set(request)
    }

    after() {
      CorrelationID.clear()
      ThreadContext.remove(CorrelationIdKey)
      AuthUser.clear()
      ApplicationUrl.clear()
    }

    override def ndlaErrorHandler: NdlaErrorHandler = {
      case a: AccessDeniedException if a.unauthorized =>
        Unauthorized(body = Error(ACCESS_DENIED, a.getMessage))
      case a: AccessDeniedException => Forbidden(body = Error(ACCESS_DENIED, a.getMessage))
      case v: ValidationException =>
        BadRequest(body = ValidationError(VALIDATION, VALIDATION_DESCRIPTION, messages = v.errors))
      case _: IndexNotFoundException                    => InternalServerError(body = IndexMissingError)
      case NotFoundException(message, sl) if sl.isEmpty => NotFound(body = Error(NOT_FOUND, message))
      case NotFoundException(message, supportedLanguages) =>
        NotFound(body = Error(NOT_FOUND, message, supportedLanguages = Some(supportedLanguages)))
      case rw: ResultWindowTooLargeException =>
        UnprocessableEntity(body = Error(WINDOW_TOO_LARGE, rw.getMessage))
      case _: PSQLException =>
        DataSource.connectToDatabase()
        InternalServerError(Error(DATABASE_UNAVAILABLE, DATABASE_UNAVAILABLE_DESCRIPTION))
      case NdlaSearchException(_, Some(rf), _)
          if rf.error.rootCause
            .exists(x => x.`type` == "search_context_missing_exception" || x.reason == "Cannot parse scroll id") =>
        BadRequest(body = InvalidSearchContext)
      case t: Throwable =>
        logger.error(GenericError.toString, t)
        InternalServerError(body = ErrorHelpers.GenericError)
    }
  }

}
