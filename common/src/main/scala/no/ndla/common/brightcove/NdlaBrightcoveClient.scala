/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.brightcove

import io.circe.generic.semiauto.deriveDecoder
import io.circe.{Decoder, Json}
import io.circe.parser.*
import sttp.client3.{HttpClientSyncBackend, UriContext, basicRequest}
import no.ndla.common.configuration.HasBaseProps
import no.ndla.common.errors.{
  TokenDecodingException,
  TokenRetrievalException,
  VideoSourceParsingException,
  VideoSourceRetrievalException
}

import scala.util.{Failure, Success, Try}

case class TokenResponse(access_token: String, token_type: String, expires_in: Int)

object TokenResponse {
  implicit def decoder: Decoder[TokenResponse] = deriveDecoder[TokenResponse]
}

trait NdlaBrightcoveClient {
  this: HasBaseProps =>
  val brightcoveClient: NdlaBrightcoveClient

  class NdlaBrightcoveClient {
    private val backend = HttpClientSyncBackend()

    def getToken(clientID: String, clientSecret: String): Try[String] = {
      val request =
        basicRequest.auth
          .basic(clientID, clientSecret)
          .post(uri"${props.BrightCoveAuthUri}?grant_type=client_credentials")
      Try(request.send(backend).body) match {
        case Success(Right(jsonString)) =>
          decode[TokenResponse](jsonString) match {
            case Right(tokenResponse) => Success(tokenResponse.access_token)
            case Left(error)          =>
              Failure(new TokenDecodingException(s"Failed to decode token response: ${error.getMessage}"))
          }
        case Success(Left(error)) => Failure(new TokenRetrievalException(s"Failed to get token: ${error}"))
        case Failure(exception)   => Failure(new TokenRetrievalException(exception.getMessage))
      }
    }

    def getVideoSource(accountId: String, videoId: String, bearerToken: String): Try[Vector[Json]] = {

      val videoSourceUrl = props.BrightCoveVideoUri(accountId, videoId)
      val request        = basicRequest
        .header("Authorization", s"Bearer $bearerToken")
        .get(videoSourceUrl)

      implicit val backend = HttpClientSyncBackend()

      Try(request.send(backend).body) match {
        case Success(Right(jsonString)) =>
          parse(jsonString) match {
            case Right(json) =>
              json.asArray match {
                case Some(videoSources) => Success(videoSources)
                case None               => Failure(new VideoSourceParsingException("Failed to parse video source"))
              }
            case Left(error) =>
              Failure(new VideoSourceParsingException(s"Failed to parse video source: ${error.getMessage}"))
          }
        case Success(Left(error)) =>
          Failure(new VideoSourceRetrievalException(s"Failed to get video source: ${error}"))
        case Failure(exception) => Failure(new VideoSourceRetrievalException(exception.getMessage))
      }
    }
  }
}
