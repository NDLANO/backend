/*
 * Part of NDLA draft-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.integration

import com.typesafe.scalalogging.StrictLogging
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.draft.Draft
import no.ndla.draftapi.Props
import no.ndla.draftapi.service.ConverterService
import no.ndla.network.NdlaClient
import no.ndla.network.tapir.auth.TokenUser
import sttp.client3.quick.*

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait SearchApiClient {
  this: NdlaClient & ConverterService & Props =>
  val searchApiClient: SearchApiClient

  class SearchApiClient(SearchApiBaseUrl: String = s"http://${props.SearchApiHost}") extends StrictLogging {

    private val InternalEndpoint        = s"$SearchApiBaseUrl/intern"
    private val SearchEndpointPublished = s"$SearchApiBaseUrl/search-api/v1/search/"
    private val indexTimeout            = 60.seconds

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

    private def postWithData[A: Decoder, B <: AnyRef: Encoder](
        endpointUrl: String,
        data: B,
        user: TokenUser,
        params: (String, String)*
    )(implicit
        executionContext: ExecutionContext
    ): Future[Try[A]] = {
      Future {
        ndlaClient.fetchWithForwardedAuth[A](
          quickRequest
            .post(uri"$endpointUrl".withParams(params: _*))
            .body(CirceUtil.toJsonString(data))
            .readTimeout(indexTimeout)
            .header("content-type", "application/json", replaceExisting = true),
          Some(user)
        )
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

    private def get[A: Decoder](endpointUrl: String, user: TokenUser, params: (String, String)*): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](quickRequest.get(uri"$endpointUrl".withParams(params: _*)), Some(user))
    }
  }

}

case class SearchResults(totalCount: Int, results: Seq[SearchHit])
object SearchResults {
  implicit val encoder: Encoder[SearchResults] = deriveEncoder
  implicit val decoder: Decoder[SearchResults] = deriveDecoder
}

case class SearchHit(id: Long, title: Title)
object SearchHit {
  implicit val encoder: Encoder[SearchHit] = deriveEncoder
  implicit val decoder: Decoder[SearchHit] = deriveDecoder
}

case class Title(title: String, language: String)
object Title {
  implicit val encoder: Encoder[Title] = deriveEncoder
  implicit val decoder: Decoder[Title] = deriveDecoder
}
