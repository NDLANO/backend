/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.controller

import com.typesafe.scalalogging.LazyLogging

import javax.servlet.http.HttpServletRequest
import no.ndla.network.{ApplicationUrl, AuthUser, CorrelationID}
import no.ndla.searchapi.SearchApiProperties.{CorrelationIdHeader, CorrelationIdKey}
import no.ndla.searchapi.integration.FeideApiClient
import no.ndla.searchapi.model.api.{
  AccessDeniedException,
  Error,
  FeideApiException,
  InvalidIndexBodyException,
  ResultWindowTooLargeException,
  TaxonomyException,
  ValidationException,
  ValidationMessage
}
import no.ndla.searchapi.model.domain.NdlaSearchException
import no.ndla.searchapi.model.domain.article.LearningResourceType
import no.ndla.searchapi.model.domain.draft.ArticleStatus
import no.ndla.searchapi.model.domain.learningpath._
import org.apache.logging.log4j.ThreadContext
import org.elasticsearch.index.IndexNotFoundException
import org.json4s.Formats
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.Serialization.read
import org.scalatra._
import org.scalatra.json.NativeJsonSupport

import scala.util.{Failure, Success, Try}

abstract class NdlaController extends ScalatraServlet with NativeJsonSupport with LazyLogging {
  protected implicit override val jsonFormats: Formats =
    org.json4s.DefaultFormats +
      new EnumNameSerializer(ArticleStatus) +
      new EnumNameSerializer(LearningPathStatus) +
      new EnumNameSerializer(LearningPathVerificationStatus) +
      new EnumNameSerializer(StepType) +
      new EnumNameSerializer(StepStatus) +
      new EnumNameSerializer(EmbedType) +
      new EnumNameSerializer(LearningResourceType) ++
      org.json4s.ext.JodaTimeSerializers.all

  private val currentTimeBeforeRequest = new ThreadLocal[Long]

  before() {
    currentTimeBeforeRequest.set(System.currentTimeMillis())
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
    logger.info(
      "{} {}{} in {} with code {}",
      request.getMethod,
      request.getRequestURI,
      Option(request.getQueryString).map(s => s"?$s").getOrElse(""),
      Option(currentTimeBeforeRequest.get())
        .map(ct => System.currentTimeMillis() - ct)
        .map(s => s" in ${s}ms")
        .getOrElse(""),
      response.getStatus
    )

    CorrelationID.clear()
    ThreadContext.remove(CorrelationIdKey)
    AuthUser.clear()
    ApplicationUrl.clear()
    currentTimeBeforeRequest.remove()
  }

  error {
    case rw: ResultWindowTooLargeException => UnprocessableEntity(body = Error(Error.WINDOW_TOO_LARGE, rw.getMessage))
    case _: IndexNotFoundException         => InternalServerError(body = Error.IndexMissingError)
    case _: InvalidIndexBodyException      => BadRequest(body = Error.InvalidBody)
    case te: TaxonomyException             => InternalServerError(body = Error(Error.TAXONOMY_FAILURE, te.getMessage))
    case ade: AccessDeniedException        => Forbidden(Error(Error.ACCESS_DENIED, ade.getMessage))
    case nse: NdlaSearchException
        if nse.rf.error.rootCause.exists(x =>
          x.`type` == "search_context_missing_exception" || x.reason == "Cannot parse scroll id") =>
      BadRequest(body = Error.InvalidSearchContext)
    case t: Throwable =>
      logger.error(Error.GenericError.toString, t)
      InternalServerError(body = Error.GenericError)
  }

  private val customRenderer: RenderPipeline = {
    case Failure(e) => errorHandler(e)
    case Success(s) => s
  }

  override def renderPipeline: PartialFunction[Any, Any] = customRenderer orElse super.renderPipeline

  def paramOrNone(paramName: String)(implicit request: HttpServletRequest): Option[String] =
    params.get(paramName).map(_.trim).filterNot(_.isEmpty())

  def paramOrDefault(paramName: String, default: String)(implicit request: HttpServletRequest): String =
    paramOrNone(paramName).getOrElse(default)

  def intOrNone(paramName: String)(implicit request: HttpServletRequest): Option[Int] =
    paramOrNone(paramName).flatMap(i => Try(i.toInt).toOption)

  def intOrDefault(paramName: String, default: Int): Int =
    intOrNone(paramName).getOrElse(default)

  def paramAsListOfString(paramName: String)(implicit request: HttpServletRequest): List[String] = {
    params.get(paramName) match {
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
            errors =
              Seq(ValidationMessage(paramName, s"Invalid value for $paramName. Only (list of) digits are allowed.")))
        }
        strings.map(_.toLong)
    }
  }

  def long(paramName: String)(implicit request: HttpServletRequest): Long = {
    val paramValue = params(paramName)
    if (paramValue.forall(_.isDigit)) {
      paramValue.toLong
    } else {
      throw new ValidationException(
        errors = Seq(ValidationMessage(paramName, s"Invalid value for $paramName. Only digits are allowed.")))
    }
  }

  def booleanOrNone(paramName: String)(implicit request: HttpServletRequest): Option[Boolean] =
    paramOrNone(paramName).flatMap(p => Try(p.toBoolean).toOption)

  def booleanOrDefault(paramName: String, default: Boolean)(implicit request: HttpServletRequest): Boolean =
    booleanOrNone(paramName).getOrElse(default)

  def extract[T](json: String)(implicit mf: scala.reflect.Manifest[T]): T = {
    Try(read[T](json)) match {
      case Failure(e) =>
        logger.error(e.getMessage, e)
        throw new ValidationException(errors = Seq(ValidationMessage("body", e.getMessage)))
      case Success(data) => data
    }
  }

  case class Param[T](paramName: String, description: String)(implicit mf: Manifest[T])

}
