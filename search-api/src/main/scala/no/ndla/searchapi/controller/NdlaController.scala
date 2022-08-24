/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.controller

import enumeratum.Json4s
import no.ndla.common.errors.AccessDeniedException
import no.ndla.common.model.domain.Availability
import no.ndla.common.model.domain.draft.{ArticleType, DraftStatus}
import no.ndla.common.scalatra.NdlaControllerBase
import no.ndla.network.{ApplicationUrl, AuthUser, CorrelationID}
import no.ndla.search.{IndexNotFoundException, NdlaSearchException}
import no.ndla.searchapi.Props
import no.ndla.searchapi.model.api.{Error, ErrorHelpers, TaxonomyException}
import no.ndla.searchapi.model.domain.article.LearningResourceType
import no.ndla.searchapi.model.domain.learningpath._
import org.apache.logging.log4j.ThreadContext
import org.json4s.Formats
import org.json4s.ext.{EnumNameSerializer, JavaTimeSerializers, JavaTypesSerializers}
import org.scalatra._

import scala.util.{Failure, Success}

trait NdlaController {
  this: Props with ErrorHelpers =>

  import props.{CorrelationIdHeader, CorrelationIdKey}
  abstract class NdlaController extends NdlaControllerBase {
    protected implicit override val jsonFormats: Formats =
      org.json4s.DefaultFormats +
        new EnumNameSerializer(DraftStatus) +
        new EnumNameSerializer(LearningPathStatus) +
        new EnumNameSerializer(LearningPathVerificationStatus) +
        new EnumNameSerializer(StepType) +
        new EnumNameSerializer(StepStatus) +
        new EnumNameSerializer(EmbedType) +
        new EnumNameSerializer(LearningResourceType) +
        new EnumNameSerializer(Availability) ++
        JavaTimeSerializers.all ++
        JavaTypesSerializers.all +
        Json4s.serializer(ArticleType)

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

  }
}
