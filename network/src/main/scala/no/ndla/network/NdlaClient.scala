/*
 * Part of NDLA network
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network

import io.circe.Decoder
import io.circe.parser.parse
import no.ndla.common.CorrelationID
import no.ndla.network.model.{HttpRequestException, NdlaRequest}
import no.ndla.network.tapir.auth.TokenUser
import sttp.client3.{Response, SimpleHttpClient}
import sttp.client3.quick.*

import scala.util.{Failure, Success, Try}

class NdlaClient {
  val client: SimpleHttpClient                 = simpleHttpClient
  private val ResponseErrorBodyCharacterCutoff = 1000

  def fetch[A](request: NdlaRequest)(implicit decoder: Decoder[A]): Try[A] = {
    doFetch(addCorrelationId(request))
  }

  def fetchWithBasicAuth[A: Decoder](request: NdlaRequest, user: String, password: String): Try[A] = {
    doFetch(addCorrelationId(addBasicAuth(request, user, password)))
  }

  def fetchWithForwardedAuth[A: Decoder](request: NdlaRequest, tokenUser: Option[TokenUser]): Try[A] = {
    doFetch(addCorrelationId(addForwardedAuth(request, tokenUser)))
  }

  def fetchWithForwardedFeideAuth[A: Decoder](request: NdlaRequest, feideToken: Option[String]): Try[A] = {
    doFetch(addCorrelationId(addForwardedFeideAuth(request, feideToken)))
  }

  def fetchRaw(request: NdlaRequest): Try[Response[String]] =
    doRequest(addCorrelationId(request))

  /** Useful if response body is not json. */
  def fetchRawWithForwardedAuth(request: NdlaRequest, tokenUser: Option[TokenUser]): Try[Response[String]] = {
    doRequest(addCorrelationId(addForwardedAuth(request, tokenUser)))
  }

  private def doFetch[A: Decoder](request: NdlaRequest): Try[A] = {
    for {
      httpResponse <- doRequest(request)
      bodyObject   <- parseResponse[A](httpResponse)
    } yield bodyObject
  }

  def doRequest(request: NdlaRequest): Try[Response[String]] = {
    Try(client.send(request)).flatMap { response =>
      if (response.isSuccess) {
        Success(response)
      } else {
        Failure(
          new HttpRequestException(
            s"Received error ${response.code} ${response.statusText} when calling ${request.uri}. Body was ${response.body}",
            Some(response)
          )
        )
      }
    }
  }

  private def parseResponse[A: Decoder](response: Response[String]): Try[A] = {
    parse(response.body).flatMap(_.as[A]) match {
      case Right(extracted) => Success(extracted)
      case Left(err)        =>
        // Large bodies in the error message can be very noisy.
        // If they are actually needed the `httpResponse` field of the exception can be used
        val errBody =
          if (response.body.length > ResponseErrorBodyCharacterCutoff)
            s"'${response.body.substring(0, 1000)}'... (Cut off)"
          else response.body

        val newEx = new HttpRequestException(s"Could not parse response with body: $errBody", Some(response))
        Failure(newEx.initCause(err))
    }
  }

  private def addCorrelationId(request: NdlaRequest) = CorrelationID.get match {
    case None                => request
    case Some(correlationId) => request.header("X-Correlation-ID", correlationId)
  }

  private def addBasicAuth(request: NdlaRequest, user: String, password: String) = {
    request.auth.basic(user, password)
  }

  private def addForwardedFeideAuth(request: NdlaRequest, feideToken: Option[String]) = {
    feideToken match {
      case None        => request
      case Some(token) => request.header("FeideAuthorization", s"Bearer $token", replaceExisting = true)
    }
  }

  private def addForwardedAuth(request: NdlaRequest, tokenUser: Option[TokenUser]) = tokenUser match {
    case Some(TokenUser(_, _, _, Some(auth))) =>
      request.header("Authorization", s"Bearer $auth", replaceExisting = true)
    case _ => request
  }
}
