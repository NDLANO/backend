package no.ndla.common.brightcove

import io.circe.{Decoder, Json}
import io.circe.generic.codec.DerivedAsObjectCodec.deriveCodec
import io.circe.generic.semiauto.deriveDecoder
import io.circe.parser.*
import sttp.client3.{HttpClientSyncBackend, UriContext, basicRequest}

case class TokenResponse(access_token: String, token_type: String, expires_in: Int)
case class VideoSource(
    src: String,
    `type`: String,
    container: String,
    codec: Option[String] = None,
    encoding_rate: Option[Int] = None,
    duration: Option[Int] = None,
    height: Option[Int] = None,
    width: Option[Int] = None,
    size: Option[Long] = None,
    uploaded_at: Option[String] = None,
    ext_x_version: Option[String] = None,
    profiles: Option[String] = None,
    remote: Option[Boolean] = None
)

trait NdlaBrightcoveClient {
  val brightcoveClient: NdlaBrightcoveClient

  class NdlaBrightcoveClient {
    private val authUrl = "https://oauth.brightcove.com/v4/access_token"
    private val backend = HttpClientSyncBackend() // Or AsyncHttpClientFutureBackend()

    def getToken(clientID: String, clientSecret: String): Either[String, String] = {
      val request =
        basicRequest.auth
          .basic(clientID, clientSecret)
          .post(uri"$authUrl?grant_type=client_credentials")
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

      val videoSourceUrl = uri"https://cms.api.brightcove.com/v1/accounts/$accountId/videos/$videoId/sources"
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
