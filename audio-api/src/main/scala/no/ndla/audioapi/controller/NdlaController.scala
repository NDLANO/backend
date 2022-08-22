/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.Props
import no.ndla.audioapi.integration.DataSource
import no.ndla.audioapi.model.api._
import no.ndla.common.errors.AccessDeniedException
import no.ndla.network.model.HttpRequestException
import no.ndla.network.{ApplicationUrl, AuthUser, CorrelationID}
import no.ndla.search.NdlaSearchException
import org.apache.logging.log4j.ThreadContext
import org.postgresql.util.PSQLException
import org.scalatra._
import org.scalatra.servlet.SizeConstraintExceededException
import no.ndla.scalatra.NdlaControllerBase

trait NdlaController {
  this: Props with ErrorHelpers with DataSource =>

  abstract class NdlaController extends NdlaControllerBase {
    import props._

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
      ApplicationUrl.clear()
      AuthUser.clear()
    }

    import ErrorHelpers._
    override def ndlaErrorHandler: NdlaErrorHandler = {
      case a: AccessDeniedException          => Forbidden(body = Error(ACCESS_DENIED, a.getMessage))
      case v: ValidationException            => BadRequest(body = ValidationError(VALIDATION, messages = v.errors))
      case hre: HttpRequestException         => BadGateway(Error(REMOTE_ERROR, hre.getMessage))
      case rw: ResultWindowTooLargeException => UnprocessableEntity(body = Error(WINDOW_TOO_LARGE, rw.getMessage))
      case i: ImportException                => UnprocessableEntity(body = Error(IMPORT_FAILED, i.getMessage))
      case nfe: NotFoundException            => NotFound(body = Error(NOT_FOUND, nfe.getMessage))
      case o: OptimisticLockException        => Conflict(body = Error(RESOURCE_OUTDATED, o.getMessage))
      case _: SizeConstraintExceededException =>
        contentType = formats("json")
        RequestEntityTooLarge(body = FileTooBigError)
      case _: PSQLException =>
        DataSource.connectToDatabase()
        InternalServerError(Error(DATABASE_UNAVAILABLE, DATABASE_UNAVAILABLE_DESCRIPTION))
      case NdlaSearchException(_, Some(rf), _)
          if rf.error.rootCause
            .exists(x => x.`type` == "search_context_missing_exception" || x.reason == "Cannot parse scroll id") =>
        BadRequest(body = InvalidSearchContext)
      case t: Throwable => {
        t.printStackTrace()
        logger.error(t.getMessage)
        InternalServerError(Error(GENERIC, GENERIC_DESCRIPTION))
      }
    }
  }
}
