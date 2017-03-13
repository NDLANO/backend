/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import javax.servlet.http.HttpServletRequest

import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.{ApplicationUrl, AuthUser, CorrelationID}
import org.apache.logging.log4j.ThreadContext
import org.json4s.{DefaultFormats, Formats}
import org.scalatra._
import org.scalatra.json.NativeJsonSupport
import no.ndla.audioapi.AudioApiProperties.{CorrelationIdHeader, CorrelationIdKey}
import no.ndla.audioapi.model.api.{AccessDeniedException, Error, ValidationError, ValidationException, ValidationMessage}
import no.ndla.network.model.HttpRequestException
import org.scalatra.servlet.SizeConstraintExceededException

abstract class NdlaController extends ScalatraServlet with NativeJsonSupport with LazyLogging {
  protected implicit override val jsonFormats: Formats = DefaultFormats

  before() {
    contentType = formats("json")
    CorrelationID.set(Option(request.getHeader(CorrelationIdHeader)))
    ThreadContext.put(CorrelationIdKey, CorrelationID.get.getOrElse(""))
    ApplicationUrl.set(request)
    AuthUser.set(request)
    logger.info("{} {}{}", request.getMethod, request.getRequestURI, Option(request.getQueryString).map(s => s"?$s").getOrElse(""))
  }

  after() {
    CorrelationID.clear()
    ThreadContext.remove(CorrelationIdKey)
    ApplicationUrl.clear
    AuthUser.clear()
  }

  error {
    case a: AccessDeniedException => Forbidden(body = Error(Error.ACCESS_DENIED, a.getMessage))
    case v: ValidationException => BadRequest(body=ValidationError(messages=v.errors))
    case hre: HttpRequestException => BadGateway(Error(Error.REMOTE_ERROR, hre.getMessage))
    case _: SizeConstraintExceededException =>
      contentType = formats("json")
      RequestEntityTooLarge(body=Error.FileTooBigError)
    case t: Throwable => {
      t.printStackTrace()
      logger.error(t.getMessage)
      InternalServerError(Error(t.getMessage))
    }
  }

  def long(paramName: String)(implicit request: HttpServletRequest): Long = {
    val paramValue = params(paramName)
    paramValue.forall(_.isDigit) match {
      case true => paramValue.toLong
      case false => throw new ValidationException(errors=Seq(ValidationMessage("parameter", s"Invalid value for $paramName. Only digits are allowed.")))
    }
  }

  def paramOrNone(paramName: String)(implicit request: HttpServletRequest): Option[String] = {
    params.get(paramName).map(_.trim).filterNot(_.isEmpty())
  }

  def assertHasRole(role: String): Unit = {
    if (!AuthUser.hasRole(role))
      throw new AccessDeniedException("User is missing required role to perform this operation")
  }

}
