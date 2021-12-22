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
import no.ndla.conceptapi.ConceptApiProperties.{
  CorrelationIdHeader,
  CorrelationIdKey,
  ElasticSearchIndexMaxResultWindow,
  ElasticSearchScrollKeepAlive,
  InitialScrollContextKeywords
}
import no.ndla.conceptapi.auth.User
import no.ndla.conceptapi.model.api.{
  Concept,
  Error,
  IllegalStatusStateTransition,
  NewConcept,
  NotFoundException,
  OperationNotAllowedException,
  OptimisticLockException,
  ResultWindowTooLargeException,
  ValidationError
}
import no.ndla.conceptapi.model.domain.NdlaSearchException
import no.ndla.conceptapi.service.WriteService
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
import org.scalatra.swagger.{ParamType, Parameter, ResponseMessage, SwaggerSupport}

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
    ApplicationUrl.clear()
  }

  case class Param[T](paramName: String, description: String)(implicit mf: Manifest[T])

  error {
    case a: AccessDeniedException         => Forbidden(body = Error(Error.ACCESS_DENIED, a.getMessage))
    case v: ValidationException           => BadRequest(body = ValidationError(messages = v.errors))
    case n: NotFoundException             => NotFound(body = Error(Error.NOT_FOUND, n.getMessage))
    case o: OptimisticLockException       => Conflict(body = Error(Error.RESOURCE_OUTDATED, o.getMessage))
    case st: IllegalStatusStateTransition => BadRequest(body = Error(Error.VALIDATION, st.getMessage))
    case e: IndexNotFoundException        => InternalServerError(body = Error.IndexMissingError)
    case nse: NdlaSearchException
        if nse.rf.error.rootCause.exists(x =>
          x.`type` == "search_context_missing_exception" || x.reason == "Cannot parse scroll id") =>
      BadRequest(body = Error.InvalidSearchContext)
    case ona: OperationNotAllowedException => BadRequest(body = Error(Error.OPERATION_NOT_ALLOWED, ona.getMessage))
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

  // Additional models used in error responses
  registerModel[ValidationError]()
  registerModel[Error]()

  val response400 = ResponseMessage(400, "Validation Error", Some("ValidationError"))
  val response403 = ResponseMessage(403, "Access Denied", Some("Error"))
  val response404 = ResponseMessage(404, "Not found", Some("Error"))
  val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

  protected val query =
    Param[Option[String]]("query", "Return only concepts with content matching the specified query.")
  protected val conceptId =
    Param[Long]("concept_id", "Id of the concept that is to be returned")
  protected val conceptIds = Param[Option[Seq[Long]]](
    "ids",
    "Return only concepts that have one of the provided ids. To provide multiple ids, separate by comma (,).")
  protected val correlationId =
    Param[Option[String]]("X-Correlation-ID", "User supplied correlation-id. May be omitted.")
  protected val pageNo = Param[Option[Int]]("page", "The page number of the search hits to display.")
  protected val pageSize = Param[Option[Int]]("page-size", "The number of search hits to display for each page.")
  protected val sort = Param[Option[String]](
    "sort",
    """The sorting used on results.
             The following are supported: relevance, -relevance, title, -title, lastUpdated, -lastUpdated, id, -id.
             Default is by -relevance (desc) when query is set, and title (asc) when query is empty.""".stripMargin
  )
  protected val deprecatedNodeId = Param[Long]("deprecated_node_id", "Id of deprecated NDLA node")
  protected val language = Param[Option[String]]("language", "The ISO 639-1 language code describing language.")
  protected val pathLanguage = Param[String]("language", "The ISO 639-1 language code describing language.")
  protected val license = Param[Option[String]]("license", "Return only results with provided license.")
  protected val fallback = Param[Option[Boolean]]("fallback", "Fallback to existing language if language is specified.")
  protected val scrollId = Param[Option[String]](
    "search-context",
    s"""A unique string obtained from a search you want to keep scrolling in. To obtain one from a search, provide one of the following values: ${InitialScrollContextKeywords
         .mkString("[", ",", "]")}.
       |When scrolling, the parameters from the initial search is used, except in the case of '${this.language.paramName}' and '${this.fallback.paramName}'.
       |This value may change between scrolls. Always use the one in the latest scroll result (The context, if unused, dies after $ElasticSearchScrollKeepAlive).
       |If you are not paginating past $ElasticSearchIndexMaxResultWindow hits, you can ignore this and use '${this.pageNo.paramName}' and '${this.pageSize.paramName}' instead.
       |""".stripMargin
  )
  protected val subjects =
    Param[Option[String]]("subjects", "A comma-separated list of subjects that should appear in the search.")
  protected val tagsToFilterBy =
    Param[Option[String]]("tags", "A comma-separated list of tags to filter the search by.")
  protected val userFilter = Param[Option[Seq[String]]](
    "users",
    s"""List of users to filter by.
       |The value to search for is the user-id from Auth0.""".stripMargin
  )

  protected val embedResource = Param[Option[String]]("embed-resource", "Return concepts with matching embed type.")
  protected val embedId = Param[Option[String]]("embed-id", "Return concepts with matching embed id.")

  protected val exactTitleMatch =
    Param[Option[Boolean]]("exact-match", "If provided, only return concept where query matches title exactly.")

  protected def asHeaderParam[T: Manifest: NotNothing](param: Param[T]) =
    headerParam[T](param.paramName).description(param.description)
  protected def asQueryParam[T: Manifest: NotNothing](param: Param[T]) =
    queryParam[T](param.paramName).description(param.description)
  protected def asPathParam[T: Manifest: NotNothing](param: Param[T]) =
    pathParam[T](param.paramName).description(param.description)

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

  def intOrNone(paramName: String)(implicit request: HttpServletRequest): Option[Int] =
    paramOrNone(paramName).flatMap(p => Try(p.toInt).toOption)

  def intOrDefault(paramName: String, default: Int): Int = intOrNone(paramName).getOrElse(default)

  def paramOrNone(paramName: String)(implicit request: HttpServletRequest): Option[String] = {
    params.get(paramName).map(_.trim).filterNot(_.isEmpty())
  }

  def paramOrDefault(paramName: String, default: String)(implicit request: HttpServletRequest): String = {
    paramOrNone(paramName).getOrElse(default)
  }

  def booleanOrNone(paramName: String)(implicit request: HttpServletRequest): Option[Boolean] =
    paramOrNone(paramName).flatMap(p => Try(p.toBoolean).toOption)

  def booleanOrDefault(paramName: String, default: Boolean)(implicit request: HttpServletRequest): Boolean =
    booleanOrNone(paramName).getOrElse(default)

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
            errors =
              Seq(ValidationMessage(paramName, s"Invalid value for $paramName. Only (list of) digits are allowed.")))
        }
        strings.map(_.toLong)
    }
  }

}
