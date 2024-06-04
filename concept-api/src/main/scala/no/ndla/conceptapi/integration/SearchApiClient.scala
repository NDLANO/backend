/*
 * Part of NDLA concept-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.integration

import com.typesafe.scalalogging.StrictLogging
import io.circe.{Decoder, Encoder}
import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.concept.Concept
import no.ndla.conceptapi.Props
import no.ndla.conceptapi.service.ConverterService
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

    private val InternalEndpoint = s"$SearchApiBaseUrl/intern"
    private val indexTimeout     = 60.seconds

    def indexConcept(concept: Concept, user: TokenUser)(implicit ex: ExecutionContext): Concept = {
      val future = postWithData[Concept, Concept](s"$InternalEndpoint/concept/", concept, user)
      future.onComplete {
        case Success(Success(_)) =>
          logger.info(
            s"Successfully indexed concept with id: '${concept.id.getOrElse(-1)}' and revision '${concept.revision
                .getOrElse(-1)}' in search-api"
          )
        case Failure(e) =>
          logger.error(
            s"Failed to indexed concept with id: '${concept.id.getOrElse(-1)}' and revision '${concept.revision
                .getOrElse(-1)}' in search-api",
            e
          )
        case Success(Failure(e)) =>
          logger.error(
            s"Failed to indexed concept with id: '${concept.id.getOrElse(-1)}' and revision '${concept.revision
                .getOrElse(-1)}' in search-api",
            e
          )
      }

      concept
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
  }
}
