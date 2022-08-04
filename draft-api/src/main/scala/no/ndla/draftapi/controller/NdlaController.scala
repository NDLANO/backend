/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.controller

import no.ndla.draftapi.integration.DataSource

import no.ndla.draftapi.Props
import no.ndla.draftapi.model.api.{
  AccessDeniedException,
  ArticlePublishException,
  ArticleStatusException,
  Error,
  ErrorHelpers,
  NotFoundException,
  ValidationError
}
import no.ndla.network.model.HttpRequestException
import no.ndla.network.{ApplicationUrl, AuthUser, CorrelationID}
import no.ndla.search.{IndexNotFoundException, NdlaSearchException}
import org.apache.logging.log4j.ThreadContext
import org.json4s.{DefaultFormats, Formats}
import org.postgresql.util.PSQLException
import org.scalatra._

import no.ndla.scalatra.error.ValidationException
import no.ndla.scalatra.NdlaSwaggerSupport
import no.ndla.scalatra.NdlaControllerBase

trait NdlaController {
  this: Props with ErrorHelpers with DataSource =>

  abstract class NdlaController extends NdlaControllerBase with NdlaSwaggerSupport {
    protected implicit override val jsonFormats: Formats = DefaultFormats.withLong
    import props._

    before() {
      contentType = formats("json")
      CorrelationID.set(Option(request.getHeader(CorrelationIdHeader)))
      ThreadContext.put(CorrelationIdKey, CorrelationID.get.getOrElse(""))
      ApplicationUrl.set(request)
      AuthUser.set(request)
    }

    after() {
      CorrelationID.clear()
      ThreadContext.remove(CorrelationIdKey)
      AuthUser.clear()
      ApplicationUrl.clear()
    }

    import ErrorHelpers._
    override def ndlaErrorHandler: NdlaErrorHandler = {
      case a: AccessDeniedException => Forbidden(body = Error(ACCESS_DENIED, a.getMessage))
      case v: ValidationException =>
        BadRequest(body = ValidationError(VALIDATION, VALIDATION_DESCRIPTION, messages = v.errors))
      case as: ArticleStatusException        => BadRequest(body = Error(VALIDATION, as.getMessage))
      case e: IndexNotFoundException         => InternalServerError(body = IndexMissingError)
      case n: NotFoundException              => NotFound(body = Error(NOT_FOUND, n.getMessage))
      case o: OptimisticLockException        => Conflict(body = Error(RESOURCE_OUTDATED, o.getMessage))
      case rw: ResultWindowTooLargeException => UnprocessableEntity(body = Error(WINDOW_TOO_LARGE, rw.getMessage))
      case pf: ArticlePublishException       => BadRequest(body = Error(PUBLISH, pf.getMessage))
      case st: IllegalStatusStateTransition  => BadRequest(body = Error(VALIDATION, st.getMessage))
      case psql: PSQLException =>
        logger.error(s"Got postgres exception: '${psql.getMessage}', attempting db reconnect", psql)
        DataSource.connectToDatabase()
        InternalServerError(Error(DATABASE_UNAVAILABLE, DATABASE_UNAVAILABLE_DESCRIPTION))
      case h: HttpRequestException =>
        h.httpResponse match {
          case Some(resp) if resp.is4xx => BadRequest(body = resp.body)
          case _ =>
            logger.error(s"Problem with remote service: ${h.getMessage}")
            BadGateway(body = GenericError)
        }
      case NdlaSearchException(_, Some(rf), _)
          if rf.error.rootCause
            .exists(x => x.`type` == "search_context_missing_exception" || x.reason == "Cannot parse scroll id") =>
        BadRequest(body = InvalidSearchContext)
      case t: Throwable =>
        logger.error(GenericError.toString, t)
        InternalServerError(body = GenericError)
    }

    protected val correlationId =
      Param[Option[String]]("X-Correlation-ID", "User supplied correlation-id. May be omitted.")
    protected val pageNo   = Param[Option[Int]]("page", "The page number of the search hits to display.")
    protected val pageSize = Param[Option[Int]]("page-size", "The number of search hits to display for each page.")
    protected val sort = Param[Option[String]](
      "sort",
      """The sorting used on results.
             The following are supported: relevance, -relevance, title, -title, lastUpdated, -lastUpdated, id, -id.
             Default is by -relevance (desc) when query is set, and title (asc) when query is empty.""".stripMargin
    )
    protected val deprecatedNodeId = Param[Long]("deprecated_node_id", "Id of deprecated NDLA node")
    protected val language     = Param[Option[String]]("language", "The ISO 639-1 language code describing language.")
    protected val pathLanguage = Param[String]("language", "The ISO 639-1 language code describing language.")
    protected val license      = Param[Option[String]]("license", "Return only results with provided license.")
    protected val fallback =
      Param[Option[Boolean]]("fallback", "Fallback to existing language if language is specified.")
    protected val scrollId = Param[Option[String]](
      "search-context",
      s"""A unique string obtained from a search you want to keep scrolling in. To obtain one from a search, provide one of the following values: ${InitialScrollContextKeywords
          .mkString("[", ",", "]")}.
       |When scrolling, the parameters from the initial search is used, except in the case of '${this.language.paramName}' and '${this.fallback.paramName}'.
       |This value may change between scrolls. Always use the one in the latest scroll result (The context, if unused, dies after $ElasticSearchScrollKeepAlive).
       |If you are not paginating past $ElasticSearchIndexMaxResultWindow hits, you can ignore this and use '${this.pageNo.paramName}' and '${this.pageSize.paramName}' instead.
       |""".stripMargin
    )

    def doOrAccessDenied(hasAccess: Boolean)(w: => Any): Any = {
      if (hasAccess) {
        w
      } else {
        errorHandler(new AccessDeniedException("Missing user/client-id or role"))
      }
    }
  }
}
