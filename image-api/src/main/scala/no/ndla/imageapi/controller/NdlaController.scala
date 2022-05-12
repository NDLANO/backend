/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.Props
import no.ndla.imageapi.integration.DataSource
import no.ndla.imageapi.model._
import no.ndla.imageapi.model.api.{Error, ErrorHelpers, ValidationError}
import no.ndla.imageapi.model.domain.ImageStream
import no.ndla.network.{ApplicationUrl, AuthUser, CorrelationID}
import no.ndla.search.{IndexNotFoundException, NdlaSearchException}
import org.apache.logging.log4j.ThreadContext
import org.json4s.native.Serialization.read
import org.json4s.{DefaultFormats, Formats}
import org.postgresql.util.PSQLException
import org.scalatra.json.NativeJsonSupport
import org.scalatra.servlet.SizeConstraintExceededException
import org.scalatra._

import java.lang.Math.{max, min}
import javax.servlet.http.HttpServletRequest
import scala.util.{Failure, Success, Try}

trait NdlaController {
  this: Props with ErrorHelpers with DataSource =>
  import props.{CorrelationIdHeader, CorrelationIdKey}

  abstract class NdlaController extends ScalatraServlet with NativeJsonSupport with LazyLogging {
    protected implicit override val jsonFormats: Formats = DefaultFormats

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
      ApplicationUrl.clear()
      AuthUser.clear()
    }

    import ErrorHelpers._
    error {
      case v: ValidationException    => BadRequest(body = ValidationError(VALIDATION, messages = v.errors))
      case a: AccessDeniedException  => Forbidden(body = Error(ACCESS_DENIED, a.getMessage))
      case e: IndexNotFoundException => InternalServerError(body = IndexMissingError)
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

    override def renderPipeline = streamRenderer orElse super.renderPipeline

    def isInteger(value: String): Boolean = value.forall(_.isDigit)

    def isDouble(value: String): Boolean = Try(value.toDouble).isSuccess

    def isBoolean(value: String): Boolean = Try(value.toBoolean).isSuccess

    def long(paramName: String)(implicit request: HttpServletRequest): Long = {
      val paramValue = params(paramName)
      if (!isInteger(paramValue))
        throw new ValidationException(
          errors = Seq(ValidationMessage(paramName, s"Invalid value for $paramName. Only digits are allowed."))
        )

      paramValue.toLong
    }

    def extractDoubleOpt2(one: String, two: String)(implicit
        request: HttpServletRequest
    ): (Option[Double], Option[Double]) = {
      (extractDoubleOpt(one), extractDoubleOpt(two))
    }

    def extractDoubleOpt(paramName: String)(implicit request: HttpServletRequest): Option[Double] = {
      params.get(paramName) match {
        case Some(value) =>
          if (!isDouble(value))
            throw new ValidationException(
              errors = Seq(ValidationMessage(paramName, s"Invalid value for $paramName. Only numbers are allowed."))
            )

          Some(value.toDouble)
        case _ => None
      }
    }

    def extractDoubleOpts(paramNames: String*)(implicit request: HttpServletRequest): Seq[Option[Double]] = {
      paramNames.map(paramName => {
        params.get(paramName) match {
          case Some(value) =>
            if (!isDouble(value))
              throw new ValidationException(
                errors = Seq(ValidationMessage(paramName, s"Invalid value for $paramName. Only numbers are allowed."))
              )

            Some(value.toDouble)
          case _ => None
        }
      })
    }

    def booleanOrDefault(paramName: String, default: Boolean)(implicit request: HttpServletRequest): Boolean = {
      val paramValue = paramOrDefault(paramName, "")
      if (!isBoolean(paramValue)) default else paramValue.toBoolean
    }

    def paramOrNone(paramName: String)(implicit request: HttpServletRequest): Option[String] = {
      params.get(paramName).map(_.trim).filterNot(_.isEmpty())
    }

    def doubleOrNone(name: String)(implicit request: HttpServletRequest): Option[Double] = {
      paramOrNone(name).flatMap(i => Try(i.toDouble).toOption)
    }

    def intOrNone(name: String)(implicit request: HttpServletRequest): Option[Int] = {
      paramOrNone(name).flatMap(i => Try(i.toInt).toOption)
    }

    def paramOrDefault(paramName: String, default: String)(implicit request: HttpServletRequest): String = {
      paramOrNone(paramName).getOrElse(default)
    }

    def paramAsListOfString(paramName: String)(implicit request: HttpServletRequest): List[String] = {
      params.get(paramName).filter(_.nonEmpty) match {
        case None        => List.empty
        case Some(param) => param.split(",").toList.map(_.trim)
      }
    }

    def intOrDefault(paramName: String, default: Int): Int = intOrNone(paramName).getOrElse(default)

    def doubleInRange(paramName: String, from: Int, to: Int)(implicit request: HttpServletRequest): Option[Double] = {
      doubleOrNone(paramName) match {
        case Some(d) if d >= min(from, to) && d <= max(from, to) => Some(d)
        case Some(d) =>
          throw new ValidationException(
            errors = Seq(
              ValidationMessage(paramName, s"Invalid value for $paramName. Must be in range $from-$to but was $d")
            )
          )
        case None => None
      }
    }

    def tryExtract[T](json: String)(implicit mf: scala.reflect.Manifest[T]): Try[T] = {
      Try(read[T](json))
    }

    def extract[T](json: String)(implicit mf: scala.reflect.Manifest[T]): T = {
      tryExtract[T](json) match {
        case Success(data) => data
        case Failure(e) =>
          logger.error(e.getMessage, e)
          throw new ValidationException(errors = Seq(ValidationMessage("body", e.getMessage)))
      }
    }
  }
}
