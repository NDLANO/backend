/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.common.errors.AccessDeniedException
import no.ndla.imageapi.Props
import no.ndla.imageapi.integration.DataSource
import no.ndla.imageapi.model._
import no.ndla.imageapi.model.api.{Error, ErrorHelpers, ValidationError}
import no.ndla.imageapi.model.domain.ImageStream
import no.ndla.network.{ApplicationUrl, AuthUser, CorrelationID}
import no.ndla.scalatra.NdlaControllerBase
import no.ndla.scalatra.error.ValidationException
import no.ndla.search.{IndexNotFoundException, NdlaSearchException}
import org.apache.logging.log4j.ThreadContext
import org.postgresql.util.PSQLException
import org.scalatra.servlet.SizeConstraintExceededException
import org.scalatra._

trait NdlaController {
  this: Props with ErrorHelpers with DataSource =>
  import props.{CorrelationIdHeader, CorrelationIdKey}

  abstract class NdlaController extends NdlaControllerBase {
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
      case v: ValidationException    => BadRequest(body = ValidationError(VALIDATION, messages = v.errors))
      case a: AccessDeniedException  => Forbidden(body = Error(ACCESS_DENIED, a.getMessage))
      case _: IndexNotFoundException => InternalServerError(body = IndexMissingError)
      case i: ImageNotFoundException => NotFound(body = Error(NOT_FOUND, i.getMessage))
      case b: ImportException        => UnprocessableEntity(body = Error(IMPORT_FAILED, b.getMessage))
      case iu: InvalidUrlException   => BadRequest(body = Error(INVALID_URL, iu.getMessage))
      case s: ImageStorageException =>
        contentType = formats("json")
        GatewayTimeout(body = Error(GATEWAY_TIMEOUT, s.getMessage))
      case rw: ResultWindowTooLargeException => UnprocessableEntity(body = Error(WINDOW_TOO_LARGE, rw.getMessage))
      case _: SizeConstraintExceededException =>
        contentType = formats("json")
        RequestEntityTooLarge(body = FileTooBigError)
      case _: PSQLException =>
        DataSource.connectToDatabase()
        InternalServerError(DatabaseUnavailableError)
      case NdlaSearchException(_, Some(rf), _)
          if rf.error.rootCause
            .exists(x => x.`type` == "search_context_missing_exception" || x.reason == "Cannot parse scroll id") =>
        BadRequest(body = InvalidSearchContext)
      case t: Throwable =>
        logger.error(GenericError.toString, t)
        InternalServerError(body = GenericError)
    }

    private val streamRenderer: RenderPipeline = { case f: ImageStream =>
      contentType = f.contentType
      org.scalatra.util.io.copy(f.stream, response.getOutputStream)
    }

    override def renderPipeline: RenderPipeline = streamRenderer.orElse(super.renderPipeline)
  }
}
