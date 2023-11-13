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
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.draft.{DraftStatus, RevisionStatus}
import no.ndla.common.model.domain.learningpath.EmbedType
import no.ndla.common.model.domain.{ArticleType, Availability, Priority}
import no.ndla.network.scalatra.NdlaControllerBase
import no.ndla.search.{IndexNotFoundException, NdlaSearchException}
import no.ndla.searchapi.Props
import no.ndla.searchapi.model.api.{Error, ErrorHelpers, TaxonomyException}
import no.ndla.searchapi.model.domain.LearningResourceType
import no.ndla.searchapi.model.domain.learningpath._
import org.json4s.Formats
import org.json4s.ext.{EnumNameSerializer, JavaTimeSerializers, JavaTypesSerializers}
import org.scalatra._

import scala.util.{Failure, Success}

trait NdlaController {
  this: Props with ErrorHelpers with NdlaControllerBase =>

  abstract class NdlaController extends NdlaControllerBase {
    protected implicit override val jsonFormats: Formats =
      org.json4s.DefaultFormats +
        new EnumNameSerializer(LearningPathStatus) +
        new EnumNameSerializer(LearningPathVerificationStatus) +
        new EnumNameSerializer(StepType) +
        new EnumNameSerializer(StepStatus) +
        new EnumNameSerializer(EmbedType) +
        new EnumNameSerializer(LearningResourceType) +
        new EnumNameSerializer(Availability) ++
        JavaTimeSerializers.all ++
        JavaTypesSerializers.all +
        Json4s.serializer(ArticleType) +
        Json4s.serializer(RevisionStatus) +
        Json4s.serializer(DraftStatus) +
        Json4s.serializer(Priority) +
        NDLADate.Json4sSerializer

    before() {
      contentType = formats("json")
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
