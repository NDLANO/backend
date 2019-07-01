/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.controller

import java.nio.file.AccessDeniedException

import com.typesafe.scalalogging.LazyLogging
import javax.servlet.http.HttpServletRequest
import no.ndla.conceptapi.ComponentRegistry
import no.ndla.conceptapi.ConceptApiProperties.{CorrelationIdHeader, CorrelationIdKey}
import no.ndla.conceptapi.model.api.{
  Error,
  NotFoundException,
  OptimisticLockException,
  ResultWindowTooLargeException,
  ValidationError
}
import no.ndla.network.{ApplicationUrl, AuthUser, CorrelationID}
import no.ndla.network.model.HttpRequestException
import no.ndla.validation.{ValidationException, ValidationMessage}
import org.apache.logging.log4j.ThreadContext
import org.elasticsearch.index.IndexNotFoundException
import org.json4s.{DefaultFormats, Formats}
import org.json4s.native.Serialization.read
import org.postgresql.util.PSQLException
import org.scalatra._
import org.scalatra.json.NativeJsonSupport
import org.scalatra.util.NotNothing
import org.scalatra.swagger.{ParamType, Parameter, SwaggerSupport}

import scala.util.{Failure, Success, Try}

abstract class NdlaController() extends ScalatraServlet with NativeJsonSupport with LazyLogging with SwaggerSupport {
  protected implicit val jsonFormats: Formats = DefaultFormats

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
    AuthUser.clear()
    ApplicationUrl.clear
  }

  case class Param[T](paramName: String, description: String)(implicit mf: Manifest[T])

  error {
    case a: AccessDeniedException =>
      Forbidden(body = Error(Error.ACCESS_DENIED, a.getMessage))
    case v: ValidationException =>
      BadRequest(body = ValidationError(messages = v.errors))
    case n: NotFoundException =>
      NotFound(body = Error(Error.NOT_FOUND, n.getMessage))
    case o: OptimisticLockException =>
      Conflict(body = Error(Error.RESOURCE_OUTDATED, o.getMessage))
    case psqle: PSQLException =>
      ComponentRegistry.connectToDatabase()
      logger.error("Something went wrong with database connections", psqle)
      InternalServerError(Error(Error.DATABASE_UNAVAILABLE, Error.DATABASE_UNAVAILABLE_DESCRIPTION))
    case h: HttpRequestException =>
      h.httpResponse match {
        case Some(resp) if resp.is4xx => BadRequest(body = resp.body)
        case _ =>
          logger.error(s"Problem with remote service: ${h.getMessage}")
          BadGateway(body = Error.GenericError)
      }
    case t: Throwable =>
      logger.error(Error.GenericError.toString, t)
      InternalServerError(body = Error.GenericError)
  }

  protected def asHeaderParam[T: Manifest: NotNothing](param: Param[T]) =
    headerParam[T](param.paramName).description(param.description)
  protected def asQueryParam[T: Manifest: NotNothing](param: Param[T]) =
    queryParam[T](param.paramName).description(param.description)
  protected def asPathParam[T: Manifest: NotNothing](param: Param[T]) =
    pathParam[T](param.paramName).description(param.description)
  protected val correlationId =
    Param[Option[String]]("X-Correlation-ID", "User supplied correlation-id. May be omitted.")

  def extract[T](json: String)(implicit mf: scala.reflect.Manifest[T]): Try[T] = {
    Try { read[T](json) } match {
      case Failure(e) =>
        Failure(new ValidationException(errors = Seq(ValidationMessage("body", e.getMessage))))
      case Success(data) => Success(data)
    }
  }

  def doOrAccessDenied(hasAccess: Boolean)(w: => Any): Any = {
    if (hasAccess) {
      w
    } else {
      errorHandler(new AccessDeniedException("Missing user/client-id or role"))
    }
  }

  def long(paramName: String)(implicit request: HttpServletRequest): Long = {
    val paramValue = params(paramName)
    paramValue.forall(_.isDigit) match {
      case true => paramValue.toLong
      case false =>
        throw new ValidationException(
          errors = Seq(ValidationMessage(paramName, s"Invalid value for $paramName. Only digits are allowed.")))
    }
  }

  private def emptySomeToNone(lang: Option[String]): Option[String] = {
    lang.filter(_.nonEmpty)
  }

  def paramOrNone(paramName: String)(implicit request: HttpServletRequest): Option[String] = {
    params.get(paramName).map(_.trim).filterNot(_.isEmpty())
  }

  def paramOrDefault(paramName: String, default: String)(implicit request: HttpServletRequest): String = {
    paramOrNone(paramName).getOrElse(default)
  }

  def paramAsListOfString(paramName: String)(implicit request: HttpServletRequest): List[String] = {
    emptySomeToNone(params.get(paramName)) match {
      case None        => List.empty
      case Some(param) => param.split(",").toList.map(_.trim)
    }
  }

}
