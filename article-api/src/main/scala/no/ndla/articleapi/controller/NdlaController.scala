/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.integration.DataSource
import no.ndla.articleapi.Props
import no.ndla.articleapi.model.api.{AccessDeniedException, Error, ErrorHelpers, NotFoundException, ValidationError}
import no.ndla.articleapi.model.domain.emptySomeToNone
import no.ndla.network.{ApplicationUrl, AuthUser, CorrelationID}
import no.ndla.search.{IndexNotFoundException, NdlaSearchException}
import no.ndla.validation.{ValidationException, ValidationMessage}
import org.apache.logging.log4j.ThreadContext
import org.json4s.ext.JavaTimeSerializers
import org.json4s.native.Serialization.read
import org.json4s.{DefaultFormats, Formats}
import org.postgresql.util.PSQLException
import org.scalatra.json.NativeJsonSupport
import org.scalatra._

import javax.servlet.http.HttpServletRequest
import scala.util.{Failure, Success, Try}

trait NdlaController {
  this: Props with ErrorHelpers with DataSource =>

  import props._
  import ErrorHelpers._

  abstract class NdlaController extends ScalatraServlet with NativeJsonSupport with LazyLogging {
    protected implicit override val jsonFormats: Formats = DefaultFormats.withLong ++ JavaTimeSerializers.all

    before() {
      contentType = formats("json")
      CorrelationID.set(Option(request.getHeader(CorrelationIdHeader)))
      ThreadContext.put(CorrelationIdKey, CorrelationID.get.getOrElse(""))
      ApplicationUrl.set(request)
      AuthUser.set(request)
      logger.info(
        "{} {}{}",
        request.getMethod,
        request.getRequestURI,
        Option(request.getQueryString).map(s => s"?$s").getOrElse("")
      )
    }

    after() {
      CorrelationID.clear()
      ThreadContext.remove(CorrelationIdKey)
      AuthUser.clear()
      ApplicationUrl.clear()
    }

    error {
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

    val digitsOnlyError = (paramName: String) =>
      Failure(
        new ValidationException(
          errors = Seq(ValidationMessage(paramName, s"Invalid value for $paramName. Only digits are allowed."))
        )
      )

    def stringParamToLong(paramName: String, paramValue: String): Try[Long] = {
      paramValue.forall(_.isDigit) match {
        case true  => Try(paramValue.toLong).recoverWith(_ => digitsOnlyError(paramName))
        case false => digitsOnlyError(paramName)
      }
    }

    def long(paramName: String)(implicit request: HttpServletRequest): Long = {
      val paramValue = params(paramName)
      stringParamToLong(paramName, paramValue).get
    }

    def paramOrNone(paramName: String)(implicit request: HttpServletRequest): Option[String] = {
      params.get(paramName).map(_.trim).filterNot(_.isEmpty())
    }

    def paramOrDefault(paramName: String, default: String)(implicit request: HttpServletRequest): String = {
      paramOrNone(paramName).getOrElse(default)
    }

    def intOrNone(paramName: String)(implicit request: HttpServletRequest): Option[Int] =
      paramOrNone(paramName).flatMap(p => Try(p.toInt).toOption)

    def intOrDefault(paramName: String, default: Int): Int = intOrNone(paramName).getOrElse(default)

    def paramAsListOfString(paramName: String)(implicit request: HttpServletRequest): List[String] = {
      emptySomeToNone(params.get(paramName)) match {
        case None        => List.empty
        case Some(param) => param.split(",").toList.map(_.trim)
      }
    }

    def paramAsListOfLong(paramName: String)(implicit request: HttpServletRequest): List[Long] = {
      val strings = paramAsListOfString(paramName)
      strings.headOption match {
        case None => List.empty
        case Some(_) =>
          if (!strings.forall(entry => entry.forall(_.isDigit))) {
            throw new ValidationException(
              errors = Seq(
                ValidationMessage(paramName, s"Invalid value for $paramName. Only (list of) digits are allowed.")
              )
            )
          }
          strings.map(_.toLong)
      }
    }

    def booleanOrNone(paramName: String)(implicit request: HttpServletRequest): Option[Boolean] =
      paramOrNone(paramName).flatMap(p => Try(p.toBoolean).toOption)

    def booleanOrDefault(paramName: String, default: Boolean)(implicit request: HttpServletRequest): Boolean =
      booleanOrNone(paramName).getOrElse(default)

    def extract[T](json: String)(implicit mf: scala.reflect.Manifest[T]): T = {
      Try {
        read[T](json)
      } match {
        case Failure(e) =>
          logger.error(e.getMessage, e)
          throw new ValidationException(errors = Seq(ValidationMessage("body", e.getMessage)))
        case Success(data) => data
      }
    }

    case class Param[T](paramName: String, description: String)

  }

}
