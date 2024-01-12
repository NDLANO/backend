/*
 * Part of NDLA network.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.scalatra

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.RequestLogger.beforeRequestLogString
import no.ndla.common.configuration.HasBaseProps
import no.ndla.common.errors.{AccessDeniedException, ValidationException, ValidationMessage}
import no.ndla.network.model.RequestInfo
import no.ndla.network.tapir.auth.{Permission, TokenUser}
import org.json4s.ext.JavaTimeSerializers
import org.json4s.native.Serialization.read
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra._
import cats.implicits._
import no.ndla.common.model.NDLADate
import no.ndla.common.model.NDLADate.asString

import javax.servlet.http.HttpServletRequest
import scala.util.{Failure, Success, Try}

trait NdlaControllerBase {
  this: HasBaseProps =>

  trait NdlaControllerBase extends ScalatraServlet with NativeJsonSupport with StrictLogging {
    protected implicit override val jsonFormats: Formats =
      DefaultFormats ++
        JavaTimeSerializers.all +
        NDLADate.Json4sSerializer

    def requirePermissionOrAccessDenied(
        requiredPermission: Permission,
        isAllowedWithoutPermission: Boolean = false
    )(f: => Any): Any =
      if (isAllowedWithoutPermission) { f }
      else { requirePermissionOrAccessDenied(requiredPermission)(_ => f) }

    def requirePermissionOrAccessDeniedWithUser(requiredPermission: Permission)(f: TokenUser => Any): Any =
      requirePermissionOrAccessDenied(requiredPermission)(f)

    def requireUserId(f: TokenUser => Any): Any = doIfAccessTrue(_.jwt.ndla_id.isDefined)(f)

    def doIfAccessTrue(checkAccess: TokenUser => Boolean)(f: TokenUser => Any): Any =
      TokenUser.fromScalatraRequest(request) match {
        case Success(user) if checkAccess(user) => f(user)
        case Success(_)                         => errorHandler(AccessDeniedException.forbidden)
        case Failure(_)                         => errorHandler(AccessDeniedException.unauthorized)
      }

    def doWithUser(f: Option[TokenUser] => Any): Any = {
      TokenUser.fromScalatraRequest(request) match {
        case Failure(_)    => f(None)
        case Success(user) => f(Some(user))
      }
    }

    private def requirePermissionOrAccessDenied(requiredPermission: Permission)(f: TokenUser => Any): Any =
      doIfAccessTrue { user =>
        user.hasPermission(requiredPermission)
      }(f)

    type NdlaErrorHandler = PartialFunction[Throwable, ActionResult]
    def ndlaErrorHandler: NdlaErrorHandler

    before() {
      RequestInfo.fromRequest(request).setThreadContextRequestInfo()

      logger.info(
        beforeRequestLogString(request.getMethod, request.getRequestURI, request.queryString)
      )
    }

    // See: no.ndla.common.scalatra.NdlaRequestLogger for whats logged on `after`.

    error { ndlaErrorHandler }

    /** Custom renderer to allow for returning Try[T] from controller endpoints. Where `Failure(ex)` will pass `ex` to
      * `ndlaErrorHandler` to be handled like a thrown exception. And `Success(value)` will return `Ok(value)` from the
      * endpoint.
      */
    private val tryRenderer: RenderPipeline = {
      case Success(value) => Ok(value)
      case Failure(ex) =>
        Try(ndlaErrorHandler(ex)) match {
          case Failure(ex: HaltException) => renderHaltException(ex)
          case Failure(ex)                => errorHandler(ex)
          case Success(result)            => result
        }
    }
    override def renderPipeline: RenderPipeline = tryRenderer.orElse(super.renderPipeline)

    private def isInteger(value: String): Boolean = value.forall(_.isDigit)
    private def isDouble(value: String): Boolean  = Try(value.toDouble).isSuccess
    private def isBoolean(value: String): Boolean = Try(value.toBoolean).isSuccess

    def long(paramName: String)(implicit request: HttpServletRequest): Long = {
      val paramValue = params(paramName)
      if (!isInteger(paramValue))
        throw ValidationException(
          "Validation Error",
          Seq(ValidationMessage(paramName, s"Invalid value for $paramName. Only digits are allowed."))
        )

      paramValue.toLong
    }

    def int(paramName: String)(implicit request: HttpServletRequest): Try[Int] = {
      val paramValue = params(paramName)
      if (!isInteger(paramValue))
        Failure(
          ValidationException(
            "Validation Error",
            Seq(ValidationMessage(paramName, s"Invalid value for $paramName. Only digits are allowed."))
          )
        )
      else Try(paramValue.toInt)
    }

    def extractDoubleOpt2(one: String, two: String)(implicit
        request: HttpServletRequest
    ): (Option[Double], Option[Double]) = {
      (extractDoubleOpt(one), extractDoubleOpt(two))
    }

    private def extractDoubleOpt(paramName: String)(implicit request: HttpServletRequest): Option[Double] = {
      params.get(paramName) match {
        case Some(value) =>
          if (!isDouble(value))
            throw ValidationException(
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
              throw ValidationException(
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

    def booleanOrNone(paramName: String)(implicit request: HttpServletRequest): Option[Boolean] = {
      params.get(paramName).map(_.trim).filterNot(_.isEmpty).flatMap(_.toBooleanOption)
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

    def castIntOrNone(name: String)(implicit request: HttpServletRequest): Option[Int] = {
      doubleOrNone(name).map(_.toInt)
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

    def intOrDefault(paramName: String, default: Int)(implicit request: HttpServletRequest): Int =
      intOrNone(paramName).getOrElse(default)

    def doubleInRange(paramName: String, from: Int, to: Int)(implicit request: HttpServletRequest): Option[Double] = {
      doubleOrNone(paramName) match {
        case Some(d) if d >= Math.min(from, to) && d <= Math.max(from, to) => Some(d)
        case Some(d) =>
          throw ValidationException(
            errors = Seq(
              ValidationMessage(paramName, s"Invalid value for $paramName. Must be in range $from-$to but was $d")
            )
          )
        case None => None
      }
    }

    private val digitsOnlyError = (paramName: String) =>
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

    def paramAsListOfLong(paramName: String)(implicit request: HttpServletRequest): List[Long] = {
      val strings = paramAsListOfString(paramName)
      strings.headOption match {
        case None => List.empty
        case Some(_) =>
          if (!strings.forall(entry => entry.forall(_.isDigit))) {
            throw ValidationException(paramName, s"Invalid value for $paramName. Only (list of) digits are allowed.")
          }
          strings.map(_.toLong)
      }
    }

    def longOrNone(paramName: String)(implicit request: HttpServletRequest): Option[Long] =
      paramOrNone(paramName).flatMap(p => Try(p.toLong).toOption)

    def paramAsDateOrNone(paramName: String)(implicit request: HttpServletRequest): Option[NDLADate] = {
      paramOrNone(paramName).map(dateString => {
        NDLADate.fromString(dateString) match {
          case Success(date) => date
          case Failure(_) =>
            throw new ValidationException(
              errors = Seq(
                ValidationMessage(
                  paramName,
                  s"Invalid date passed. Expected format is \"${asString(NDLADate.now())}\""
                )
              )
            )
        }
      })
    }

    def tryExtract[T](json: String)(implicit formats: Formats, mf: scala.reflect.Manifest[T]): Try[T] = {
      Try(read[T](json)(formats, mf))
        .recoverWith(e => Failure(ValidationException(errors = Seq(ValidationMessage("body", e.getMessage)))))
    }

    def extract[T](json: String)(implicit formats: Formats, mf: scala.reflect.Manifest[T]): T = {
      tryExtract[T](json)(formats, mf) match {
        case Success(data) => data
        case Failure(e) =>
          logger.error(e.getMessage, e)
          throw e
      }
    }
  }
}
