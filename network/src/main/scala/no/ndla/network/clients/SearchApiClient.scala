/*
 * Part of NDLA network
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.clients

import com.typesafe.scalalogging.StrictLogging
import io.circe.{Decoder, Encoder}
import no.ndla.common.CirceUtil
import no.ndla.common.configuration.HasBaseProps
import no.ndla.common.model.api.search.{MultiSearchResultDTO, MultiSearchSummaryDTO}
import no.ndla.common.model.domain.Content
import no.ndla.network.NdlaClient
import no.ndla.network.tapir.auth.TokenUser
import sttp.client3.quick.*

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait SearchApiClient {
  this: HasBaseProps & NdlaClient =>

  val searchApiClient: SearchApiClient

  class SearchApiClient(SearchApiBaseUrl: String = s"http://${props.SearchApiHost}") extends StrictLogging {
    private val InternalEndpoint        = s"$SearchApiBaseUrl/intern"
    private val SearchEndpointPublished = s"$SearchApiBaseUrl/search-api/v1/search/"
    private val indexTimeout            = 60.seconds
    private val indexRetryCount         = 3

    def indexDocument[D <: Content: Decoder: Encoder](name: String, document: D, user: Option[TokenUser])(implicit
        ex: ExecutionContext
    ): D = {
      def attemptIndex(document: D, user: Option[TokenUser], attempt: Int): D = {
        val future = postWithData[D, D](s"$InternalEndpoint/$name/", document, user)
        future.onComplete {
          case Success(Success(_)) =>
            logger.info(
              s"Successfully indexed $name with id: '${document.id.getOrElse(-1)}' and revision '${document.revision
                  .getOrElse(-1)}' after $attempt attempts in search-api"
            )
          case Failure(_) if attempt < indexRetryCount => attemptIndex(document, user, attempt + 1)
          case Failure(e) =>
            logger.error(
              s"Failed to index $name with id: '${document.id.getOrElse(-1)}' and revision '${document.revision
                  .getOrElse(-1)}' after $attempt attempts in search-api",
              e
            )
          case Success(Failure(_)) if attempt < indexRetryCount => attemptIndex(document, user, attempt + 1)
          case Success(Failure(e)) =>
            logger.error(
              s"Failed to index $name with id: '${document.id.getOrElse(-1)}' and revision '${document.revision
                  .getOrElse(-1)}' after $attempt attempts in search-api",
              e
            )
        }
        document
      }
      attemptIndex(document, user, 1)
    }

    def deleteDocument(id: Long, name: String): Long = {
      ndlaClient.doRequest(
        quickRequest
          .delete(uri"$InternalEndpoint/$name/$id")
          .readTimeout(indexTimeout)
      ): Unit
      id
    }

    private def postWithData[A: Decoder, B <: AnyRef: Encoder](
        endpointUrl: String,
        data: B,
        user: Option[TokenUser],
        params: (String, String)*
    )(implicit
        executionContext: ExecutionContext
    ): Future[Try[A]] = {
      Future {
        ndlaClient.fetchWithForwardedAuth[A](
          quickRequest
            .post(uri"$endpointUrl".withParams(params*))
            .body(CirceUtil.toJsonString(data))
            .readTimeout(indexTimeout)
            .header("content-type", "application/json", replaceExisting = true),
          user
        )
      }
    }

    def publishedWhereUsed(articleId: Long, user: TokenUser): Seq[MultiSearchSummaryDTO] = {
      get[MultiSearchResultDTO](
        SearchEndpointPublished,
        user,
        "embed-resource" -> "content-link,related-content",
        "embed-id"       -> s"$articleId"
      ) match {
        case Success(value) => value.results.collect { case x: MultiSearchSummaryDTO => x }
        case Failure(_)     => Seq.empty
      }
    }

    private def get[A: Decoder](endpointUrl: String, user: TokenUser, params: (String, String)*): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](quickRequest.get(uri"$endpointUrl".withParams(params*)), Some(user))
    }

    def convertGrepCodes(grepCodes: Seq[String], user: TokenUser): Try[Map[String, String]] = {
      get[Map[String, String]](
        s"${SearchEndpointPublished}grep/replacements",
        user,
        "codes" -> grepCodes.mkString(",")
      )
    }
  }
}
