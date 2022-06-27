/*
 * Part of NDLA scalatra.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.scalatra

import com.typesafe.scalalogging.LazyLogging
import no.ndla.scalatra.error.{ValidationException, ValidationMessage}
import org.json4s.native.Serialization.read
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.{ActionResult, HaltException, Ok, RenderPipeline, ScalatraServlet}

import javax.servlet.http.HttpServletRequest
import scala.util.{Failure, Success, Try}

trait NdlaControllerBase extends ScalatraServlet with NativeJsonSupport with LazyLogging {
  protected implicit override val jsonFormats: Formats = DefaultFormats

  type NdlaErrorHandler = PartialFunction[Throwable, ActionResult]
  def ndlaErrorHandler: NdlaErrorHandler

  private val currentTimeBeforeRequest = new ThreadLocal[Long]
  before() {
    currentTimeBeforeRequest.set(System.currentTimeMillis())
    logger.info(
      "{} {}{}",
      request.getMethod,
      request.getRequestURI,
      Option(request.getQueryString).map(s => s"?$s").getOrElse("")
    )
  }

  after() {
    logger.info(
      "{} {}{} executed in {} with code {}",
      request.getMethod,
      request.getRequestURI,
      Option(request.getQueryString).map(s => s"?$s").getOrElse(""),
      Option(currentTimeBeforeRequest.get())
        .map(ct => System.currentTimeMillis() - ct)
        .map(s => s"${s}ms")
        .getOrElse(""),
      response.getStatus
    )
  }

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

  def isInteger(value: String): Boolean = value.forall(_.isDigit)
  def isDouble(value: String): Boolean  = Try(value.toDouble).isSuccess
  def isBoolean(value: String): Boolean = Try(value.toBoolean).isSuccess

  def long(paramName: String)(implicit request: HttpServletRequest): Long = {
    val paramValue = params(paramName)
    if (!isInteger(paramValue))
      throw ValidationException(
        "Validation Error",
        Seq(ValidationMessage(paramName, s"Invalid value for $paramName. Only digits are allowed."))
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

  def tryExtract[T](json: String)(implicit mf: scala.reflect.Manifest[T]): Try[T] = {
    Try(read[T](json))
  }

  def extract[T](json: String)(implicit mf: scala.reflect.Manifest[T]): T = {
    tryExtract[T](json) match {
      case Success(data) => data
      case Failure(e) =>
        logger.error(e.getMessage, e)
        throw ValidationException(errors = Seq(ValidationMessage("body", e.getMessage)))
    }
  }
}
