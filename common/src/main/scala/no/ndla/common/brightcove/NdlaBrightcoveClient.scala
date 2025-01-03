/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.brightcove

import io.circe.Json
import io.circe.generic.codec.DerivedAsObjectCodec.deriveCodec
import io.circe.parser.*
import sttp.client3.{HttpClientSyncBackend, UriContext, basicRequest}
import no.ndla.common.configuration.HasBaseProps

case class TokenResponse(access_token: String, token_type: String, expires_in: Int)

trait NdlaBrightcoveClient {
  this: HasBaseProps =>
  val brightcoveClient: NdlaBrightcoveClient

  class NdlaBrightcoveClient {
    private val backend = HttpClientSyncBackend()

    def getToken(clientID: String, clientSecret: String): Either[String, String] = {
      val request =
        basicRequest.auth
          .basic(clientID, clientSecret)
          .post(uri"${props.BrightCoveAuthUri}?grant_type=client_credentials")
      val authResponse = request.send(backend)

      authResponse.body match {
        case Right(jsonString) =>
          decode[TokenResponse](jsonString) match {
            case Right(tokenResponse) => Right(tokenResponse.access_token)
            case Left(error)          => Left(s"Failed to decode token response: ${error.getMessage}")
          }
        case Left(error) => Left(s"Failed to get token: ${error}")
      }
    }

    def getVideoSource(accountId: String, videoId: String, bearerToken: String): Either[String, Vector[Json]] = {

      val videoSourceUrl = props.BrightCoveVideoUri(accountId, videoId)
      val request = basicRequest
        .header("Authorization", s"Bearer $bearerToken")
        .get(videoSourceUrl)

      implicit val backend = HttpClientSyncBackend()

      val response = request.send(backend)

      response.body match {
        case Right(jsonString) =>
          parse(jsonString) match {
            case Right(json) =>
              json.asArray match {
                case Some(videoSources) => Right(videoSources)
                case None               => Left("Expected a JSON array but got something else.")
              }
            case Left(error) => Left(s"Failed to decode video source response: ${error.getMessage}")
          }
        case Left(error) => Left(s"Failed to get video source: ${error}")
      }
    }
  }
}
