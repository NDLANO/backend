/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import com.typesafe.scalalogging.LazyLogging
import javax.servlet.http.HttpServletRequest
import no.ndla.audioapi.AudioApiProperties.{CorrelationIdHeader, CorrelationIdKey}
import no.ndla.audioapi.ComponentRegistry
import no.ndla.audioapi.model.api._
import no.ndla.audioapi.model.domain.NdlaSearchException
import no.ndla.network.model.HttpRequestException
import no.ndla.network.{ApplicationUrl, AuthUser, CorrelationID}
import org.apache.logging.log4j.ThreadContext
import org.json4s.{DefaultFormats, Formats}
import org.json4s.native.Serialization.read
import org.postgresql.util.PSQLException
import org.scalatra._
import org.scalatra.json.NativeJsonSupport
import org.scalatra.servlet.SizeConstraintExceededException

import scala.util.{Failure, Success, Try}

abstract class NdlaController extends ScalatraServlet with NativeJsonSupport with LazyLogging {
  protected implicit override val jsonFormats: Formats = DefaultFormats

  before() {
    contentType = formats("json")
    CorrelationID.set(Option(request.getHeader(CorrelationIdHeader)))
    ThreadContext.put(CorrelationIdKey, CorrelationID.get.getOrElse(""))
    ApplicationUrl.set(request)
    AuthUser.set(request)
    logger.info("{} {}{}",
                request.getMethod,
                request.getRequestURI,
                Option(request.getQueryString).map(s => s"?$s").getOrElse(""))
  }

  after() {
    CorrelationID.clear()
    ThreadContext.remove(CorrelationIdKey)
    ApplicationUrl.clear()
    AuthUser.clear()
  }

  error {
    case a: AccessDeniedException          => Forbidden(body = Error(Error.ACCESS_DENIED, a.getMessage))
    case v: ValidationException            => BadRequest(body = ValidationError(messages = v.errors))
    case hre: HttpRequestException         => BadGateway(Error(Error.REMOTE_ERROR, hre.getMessage))
    case rw: ResultWindowTooLargeException => UnprocessableEntity(body = Error(Error.WINDOW_TOO_LARGE, rw.getMessage))
    case i: ImportException                => UnprocessableEntity(body = Error(Error.IMPORT_FAILED, i.getMessage))
    case nfe: NotFoundException            => NotFound(body = Error(Error.NOT_FOUND, nfe.getMessage))
    case o: OptimisticLockException        => Conflict(body = Error(Error.RESOURCE_OUTDATED, o.getMessage))
    case _: SizeConstraintExceededException =>
      contentType = formats("json")
      RequestEntityTooLarge(body = Error.FileTooBigError)
    case _: PSQLException =>
      ComponentRegistry.connectToDatabase()
      InternalServerError(Error(Error.DATABASE_UNAVAILABLE, Error.DATABASE_UNAVAILABLE_DESCRIPTION))
    case nse: NdlaSearchException
        if nse.rf.error.rootCause.exists(x =>
          x.`type` == "search_context_missing_exception" || x.reason == "Cannot parse scroll id") =>
      BadRequest(body = Error.InvalidSearchContext)
    case t: Throwable => {
      t.printStackTrace()
      logger.error(t.getMessage)
      InternalServerError(Error())
    }
  }

  private val tryRenderer: RenderPipeline = {
    case Failure(ex)  => errorHandler(ex)
    case Success(res) => res
  }

  override def renderPipeline = tryRenderer orElse super.renderPipeline

  def long(paramName: String)(implicit request: HttpServletRequest): Long = {
    val paramValue = params(paramName)
    paramValue.forall(_.isDigit) match {
      case true => paramValue.toLong
      case false =>
        throw new ValidationException(
          errors = Seq(ValidationMessage("parameter", s"Invalid value for $paramName. Only digits are allowed.")))
    }
  }

  def paramOrNone(paramName: String)(implicit request: HttpServletRequest): Option[String] = {
    params.get(paramName).map(_.trim).filterNot(_.isEmpty)
  }

  def booleanOrNone(paramName: String)(implicit request: HttpServletRequest): Option[Boolean] = {
    params.get(paramName).map(_.trim).filterNot(_.isEmpty).flatMap(_.toBooleanOption)
  }

  def intOrNone(name: String)(implicit request: HttpServletRequest): Option[Int] =
    paramOrNone(name).flatMap(i => Try(i.toInt).toOption)

  def paramOrDefault(paramName: String, default: String)(implicit request: HttpServletRequest): String =
    paramOrNone(paramName).getOrElse(default)

  def intOrDefault(paramName: String, default: Int): Int = intOrNone(paramName).getOrElse(default)

  def extract[T](json: String)(implicit mf: scala.reflect.Manifest[T]): T = {
    Try(read[T](json)) match {
      case Success(data) => data
      case Failure(e) =>
        logger.error(e.getMessage, e)
        throw new ValidationException(errors = Seq(ValidationMessage("body", e.getMessage)))
    }
  }
}
