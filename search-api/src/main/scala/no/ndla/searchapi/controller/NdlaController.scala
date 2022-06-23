/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.controller

import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.{ApplicationUrl, AuthUser, CorrelationID}
import no.ndla.search.{IndexNotFoundException, NdlaSearchException}
import no.ndla.searchapi.Props
import no.ndla.searchapi.model.api.{AccessDeniedException, Error, ErrorHelpers, TaxonomyException, ValidationException, ValidationMessage}
import no.ndla.searchapi.model.domain.article.{Availability, LearningResourceType}
import no.ndla.searchapi.model.domain.draft.ArticleStatus
import no.ndla.searchapi.model.domain.learningpath._
import org.apache.logging.log4j.ThreadContext
import org.json4s.Formats
import org.json4s.ext.{EnumNameSerializer, JavaTimeSerializers}
import org.json4s.native.Serialization.read
import org.scalatra._
import org.scalatra.json.NativeJsonSupport

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.servlet.http.HttpServletRequest
import scala.util.{Failure, Success, Try}

trait NdlaController {
  this: Props with ErrorHelpers =>

  import props.{CorrelationIdHeader, CorrelationIdKey}
  abstract class NdlaController extends ScalatraServlet with NativeJsonSupport with LazyLogging {
    protected implicit override val jsonFormats: Formats =
      org.json4s.DefaultFormats +
        new EnumNameSerializer(ArticleStatus) +
        new EnumNameSerializer(LearningPathStatus) +
        new EnumNameSerializer(LearningPathVerificationStatus) +
        new EnumNameSerializer(StepType) +
        new EnumNameSerializer(StepStatus) +
        new EnumNameSerializer(EmbedType) +
        new EnumNameSerializer(LearningResourceType) +
        new EnumNameSerializer(Availability) ++
        org.json4s.ext.JodaTimeSerializers.all ++
        JavaTimeSerializers.all

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
          .map(s => s"${s}ms")
          .getOrElse(""),
        response.getStatus
      )

      CorrelationID.clear()
      ThreadContext.remove(CorrelationIdKey)
      AuthUser.clear()
      ApplicationUrl.clear()
      currentTimeBeforeRequest.remove()
    }

    import ErrorHelpers._
    error {
      case rw: ResultWindowTooLargeException => UnprocessableEntity(body = Error(WINDOW_TOO_LARGE, rw.getMessage))
      case _: IndexNotFoundException         => InternalServerError(body = IndexMissingError)
      case _: InvalidIndexBodyException      => BadRequest(body = InvalidBody)
      case te: TaxonomyException             => InternalServerError(body = Error(TAXONOMY_FAILURE, te.getMessage))
      case ade: AccessDeniedException        => Forbidden(Error(ACCESS_DENIED, ade.getMessage))
      case NdlaSearchException(_, Some(rf), _)
          if rf.error.rootCause
            .exists(x => x.`type` == "search_context_missing_exception" || x.reason == "Cannot parse scroll id") =>
        BadRequest(body = InvalidSearchContext)
      case t: Throwable =>
        logger.error(GenericError.toString, t)
        InternalServerError(body = GenericError)
    }

    private val customRenderer: RenderPipeline = {
      case Failure(e) => errorHandler(e)
      case Success(s) => s
    }

    override def renderPipeline: PartialFunction[Any, Any] = customRenderer orElse super.renderPipeline

    def paramOrNone(paramName: String)(implicit request: HttpServletRequest): Option[String] =
      params.get(paramName).map(_.trim).filterNot(_.isEmpty())

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    def paramAsDateOrNone(paramName: String)(implicit request: HttpServletRequest): Option[LocalDateTime] = {
      paramOrNone(paramName).map(dateString => {
        Try(LocalDateTime.parse(dateString, dateFormatter)) match {
          case Success(date) => date
          case Failure(_) =>
            throw new ValidationException(
              errors = Seq(
                ValidationMessage(
                  paramName,
                  s"Invalid date passed. Expected format is \"${LocalDateTime.now().format(dateFormatter)}\""
                )
              )
            )
        }
      })
    }

    def paramOrDefault(paramName: String, default: String)(implicit request: HttpServletRequest): String =
      paramOrNone(paramName).getOrElse(default)

    def intOrNone(paramName: String)(implicit request: HttpServletRequest): Option[Int] =
      paramOrNone(paramName).flatMap(i => Try(i.toInt).toOption)

    def intOrDefault(paramName: String, default: Int)(implicit request: HttpServletRequest): Int =
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
              errors = Seq(
                ValidationMessage(paramName, s"Invalid value for $paramName. Only (list of) digits are allowed.")
              )
            )
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
          errors = Seq(ValidationMessage(paramName, s"Invalid value for $paramName. Only digits are allowed."))
        )
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

    case class Param[T](paramName: String, description: String)

  }
}
