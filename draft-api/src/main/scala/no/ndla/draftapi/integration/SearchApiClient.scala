/*
 * Part of NDLA draft-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.integration

import com.typesafe.scalalogging.StrictLogging
import enumeratum.Json4s
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.{ArticleType, Availability, Priority}
import no.ndla.common.model.domain.draft.{Draft, DraftStatus, RevisionStatus}
import no.ndla.draftapi.Props
import no.ndla.draftapi.service.ConverterService
import no.ndla.network.NdlaClient
import no.ndla.network.tapir.auth.TokenUser
import org.json4s.Formats
import org.json4s.ext.{EnumNameSerializer, JavaTimeSerializers, JavaTypesSerializers}
import org.json4s.native.Serialization
import sttp.client3.quick._

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait SearchApiClient {
  this: NdlaClient with ConverterService with Props =>
  val searchApiClient: SearchApiClient

  class SearchApiClient(SearchApiBaseUrl: String = s"http://${props.SearchApiHost}") extends StrictLogging {

    private val InternalEndpoint        = s"$SearchApiBaseUrl/intern"
    private val SearchEndpoint          = s"$SearchApiBaseUrl/search-api/v1/search/editorial/"
    private val SearchEndpointPublished = s"$SearchApiBaseUrl/search-api/v1/search/"
    private val indexTimeout            = 60.seconds

    implicit val formats: Formats =
      org.json4s.DefaultFormats +
        new EnumNameSerializer(Availability) +
        Json4s.serializer(DraftStatus) +
        Json4s.serializer(ArticleType) +
        Json4s.serializer(Priority) +
        Json4s.serializer(RevisionStatus) ++
        JavaTimeSerializers.all ++
        JavaTypesSerializers.all +
        NDLADate.Json4sSerializer

    def indexDraft(draft: Draft, user: TokenUser)(implicit ex: ExecutionContext): Draft = {
      val future = postWithData[Draft, Draft](s"$InternalEndpoint/draft/", draft, user)
      future.onComplete {
        case Success(Success(_)) =>
          logger.info(
            s"Successfully indexed draft with id: '${draft.id.getOrElse(-1)}' and revision '${draft.revision.getOrElse(-1)}' in search-api"
          )
        case Failure(e) =>
          logger.error(
            s"Failed to indexed draft with id: '${draft.id.getOrElse(-1)}' and revision '${draft.revision.getOrElse(-1)}' in search-api",
            e
          )
        case Success(Failure(e)) =>
          logger.error(
            s"Failed to indexed draft with id: '${draft.id.getOrElse(-1)}' and revision '${draft.revision.getOrElse(-1)}' in search-api",
            e
          )
      }

      draft
    }

    private def postWithData[A, B <: AnyRef](endpointUrl: String, data: B, user: TokenUser, params: (String, String)*)(
        implicit
        mf: Manifest[A],
        formats: org.json4s.Formats,
        executionContext: ExecutionContext
    ): Future[Try[A]] = {
      Future {
        ndlaClient.fetchWithForwardedAuth[A](
          quickRequest
            .post(uri"$endpointUrl".withParams(params: _*))
            .body(Serialization.write(data))
            .readTimeout(indexTimeout)
            .header("content-type", "application/json", replaceExisting = true),
          Some(user)
        )
      }
    }

    def draftsWhereUsed(articleId: Long, user: TokenUser): Seq[SearchHit] = {
      get[SearchResults](
        SearchEndpoint,
        user,
        "embed-resource" -> "content-link,related-content",
        "embed-id"       -> s"${articleId}"
      ) match {
        case Success(value) => value.results
        case Failure(_)     => Seq.empty
      }
    }

    def publishedWhereUsed(articleId: Long, user: TokenUser): Seq[SearchHit] = {
      get[SearchResults](
        SearchEndpointPublished,
        user,
        "embed-resource" -> "content-link,related-content",
        "embed-id"       -> s"${articleId}"
      ) match {
        case Success(value) => value.results
        case Failure(_)     => Seq.empty
      }
    }

    private def get[A](endpointUrl: String, user: TokenUser, params: (String, String)*)(implicit
        mf: Manifest[A],
        formats: org.json4s.Formats
    ): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](quickRequest.get(uri"$endpointUrl".withParams(params: _*)), Some(user))
    }
  }

}

case class SearchResults(totalCount: Int, results: Seq[SearchHit])
case class SearchHit(id: Long, title: Title)
case class Title(title: String, language: String)
