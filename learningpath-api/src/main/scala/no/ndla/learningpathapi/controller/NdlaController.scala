/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.controller

import no.ndla.common.errors.ValidationException
import no.ndla.common.scalatra.NdlaControllerBase
import javax.servlet.http.HttpServletRequest
import no.ndla.learningpathapi.integration.DataSource
import no.ndla.learningpathapi.model.api.{Error, ErrorHelpers, ImportReport}
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.service.ConverterService
import no.ndla.network.model.HttpRequestException
import no.ndla.network.{ApplicationUrl, AuthUser}
import no.ndla.search.{IndexNotFoundException, NdlaSearchException}
import org.json4s.{DefaultFormats, Formats}
import org.postgresql.util.PSQLException
import org.scalatra._

import java.util.UUID
import scala.util.{Failure, Success, Try}
import no.ndla.learningpathapi.model.api.ValidationError

trait NdlaController {
  this: DataSource with ErrorHelpers with CorrelationIdSupport with ConverterService =>

  abstract class NdlaController extends NdlaControllerBase with CorrelationIdSupport {
    protected implicit override val jsonFormats: Formats = DefaultFormats

    before() {
      contentType = formats("json")
      ApplicationUrl.set(request)
      AuthUser.set(request)
    }

    after() {
      ApplicationUrl.clear()
      AuthUser.clear()
    }

    // This lets us return Try[T] and handle errors automatically, otherwise return 200 OK :^)
    val tryRenderer: RenderPipeline = {
      case Success(value) => value
      case Failure(ex) =>
        Try(errorHandler(ex)) match {
          case Failure(ex: HaltException) => renderHaltException(ex)
          case Failure(ex)                => errorHandler(ex)
          case Success(result)            => result
        }
    }

    override def renderPipeline: RenderPipeline = tryRenderer.orElse(super.renderPipeline)

    import ErrorHelpers._

    override def ndlaErrorHandler: NdlaErrorHandler = {
      case v: ValidationException =>
        BadRequest(body = ValidationError(VALIDATION, VALIDATION_DESCRIPTION, messages = v.errors))
      case a: AccessDeniedException =>
        Forbidden(body = Error(ACCESS_DENIED, a.getMessage))
      case dfe: DeleteFavoriteException =>
        BadRequest(body = Error(DELETE_FAVORITE, dfe.getMessage))
      case ole: OptimisticLockException =>
        Conflict(body = Error(RESOURCE_OUTDATED, RESOURCE_OUTDATED_DESCRIPTION))
      case nfe: NotFoundException =>
        NotFound(Error(NOT_FOUND, nfe.getMessage))
      case hre: HttpRequestException =>
        BadGateway(body = Error(REMOTE_ERROR, hre.getMessage))
      case i: ImportException =>
        UnprocessableEntity(body = Error(IMPORT_FAILED, i.getMessage))
      case rw: ResultWindowTooLargeException =>
        UnprocessableEntity(body = Error(WINDOW_TOO_LARGE, rw.getMessage))
      case e: IndexNotFoundException =>
        InternalServerError(body = IndexMissingError)
      case i: ElasticIndexingException =>
        InternalServerError(body = Error(GENERIC, i.getMessage))
      case ir: ImportReport => UnprocessableEntity(body = ir)
      case _: PSQLException =>
        DataSource.connectToDatabase()
        InternalServerError(DatabaseUnavailableError)
      case mse: InvalidStatusException =>
        BadRequest(Error(MISSING_STATUS, mse.getMessage))
      case NdlaSearchException(_, Some(rf), _)
          if rf.error.rootCause
            .exists(x => x.`type` == "search_context_missing_exception" || x.reason == "Cannot parse scroll id") =>
        BadRequest(body = InvalidSearchContext)
      case t: Throwable =>
        t.printStackTrace()
        logger.error(t.getMessage)
        InternalServerError(body = Error(GENERIC, GENERIC_DESCRIPTION))
    }

    def requireUserId(implicit request: HttpServletRequest): String = {
      AuthUser.get match {
        case Some(user) => user
        case None =>
          logger.warn(s"Request made to ${request.getRequestURI} without authorization")
          throw AccessDeniedException("You do not have access to the requested resource.")
      }
    }

    def uuidParam(paramName: String)(implicit request: HttpServletRequest): Try[UUID] = {
      val maybeParam = paramOrNone(paramName)(request)
      converterService.toUUIDValidated(maybeParam, paramName)
    }

    def doOrAccessDenied(hasAccess: Boolean, reason: String = "Missing user/client-id or role")(w: => Any): Any = {
      if (hasAccess) {
        w
      } else {
        errorHandler(AccessDeniedException(reason))
      }
    }
  }
}
