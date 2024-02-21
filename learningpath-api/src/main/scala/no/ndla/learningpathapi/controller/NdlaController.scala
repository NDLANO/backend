/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.controller

import no.ndla.common.errors.{AccessDeniedException, NotFoundException, ValidationException}
import no.ndla.learningpathapi.integration.DataSource
import no.ndla.learningpathapi.model.api.{Error, ErrorHelpers, ValidationError}
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.service.ConverterService
import no.ndla.myndla.model.domain.InvalidStatusException
import no.ndla.network.model.HttpRequestException
import no.ndla.network.scalatra.NdlaSwaggerSupport
import no.ndla.search.{IndexNotFoundException, NdlaSearchException}
import org.json4s.{DefaultFormats, Formats}
import org.postgresql.util.PSQLException
import org.scalatra._

import scala.util.{Failure, Success, Try}

trait NdlaController {
  this: DataSource with ErrorHelpers with ConverterService with NdlaSwaggerSupport =>

  abstract class NdlaController extends NdlaSwaggerSupport {
    protected implicit override val jsonFormats: Formats = DefaultFormats

    before() {
      contentType = formats("json")
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
      case _: OptimisticLockException =>
        Conflict(body = Error(RESOURCE_OUTDATED, RESOURCE_OUTDATED_DESCRIPTION))
      case nfe: NotFoundException =>
        NotFound(Error(NOT_FOUND, nfe.getMessage))
      case hre: HttpRequestException =>
        BadGateway(body = Error(REMOTE_ERROR, hre.getMessage))
      case i: ImportException =>
        UnprocessableEntity(body = Error(IMPORT_FAILED, i.getMessage))
      case rw: ResultWindowTooLargeException =>
        UnprocessableEntity(body = Error(WINDOW_TOO_LARGE, rw.getMessage))
      case _: IndexNotFoundException =>
        InternalServerError(body = IndexMissingError)
      case i: ElasticIndexingException =>
        InternalServerError(body = Error(GENERIC, i.getMessage))
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
        logger.error(t.getMessage, t)
        InternalServerError(body = Error(GENERIC, GENERIC_DESCRIPTION))
    }
  }
}
