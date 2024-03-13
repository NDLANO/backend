/*
 * Part of NDLA network.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network

import no.ndla.common.CorrelationID
import no.ndla.network.model.{HttpRequestException, NdlaRequest}
import no.ndla.network.tapir.auth.TokenUser
import org.json4s.Formats
import org.json4s.jackson.JsonMethods._
import org.json4s.jvalue2monadic
import org.json4s.jvalue2extractable
import sttp.client3.{Response, SimpleHttpClient}
import sttp.client3.quick._

import scala.util.{Failure, Success, Try}

trait NdlaClient {
  val ndlaClient: NdlaClient

  class NdlaClient {
    val client: SimpleHttpClient                 = simpleHttpClient
    implicit val formats: Formats                = org.json4s.DefaultFormats
    private val ResponseErrorBodyCharacterCutoff = 1000

    def fetch[A](request: NdlaRequest)(implicit formats: Formats, mf: Manifest[A]): Try[A] = {
      doFetch(addCorrelationId(request))
    }

    def fetchWithBasicAuth[A](request: NdlaRequest, user: String, password: String)(implicit
        mf: Manifest[A],
        formats: Formats = formats
    ): Try[A] = {
      doFetch(addCorrelationId(addBasicAuth(request, user, password)))
    }

    def fetchWithForwardedAuth[A](
        request: NdlaRequest,
        tokenUser: Option[TokenUser]
    )(implicit mf: Manifest[A], formats: Formats = formats): Try[A] = {
      doFetch(addCorrelationId(addForwardedAuth(request, tokenUser)))
    }

    /** Useful if response body is not json. */
    def fetchRawWithForwardedAuth(request: NdlaRequest, tokenUser: Option[TokenUser]): Try[Response[String]] = {
      doRequest(addCorrelationId(addForwardedAuth(request, tokenUser)))
    }

    private def doFetch[A](request: NdlaRequest)(implicit mf: Manifest[A], formats: Formats): Try[A] = {
      for {
        httpResponse <- doRequest(request)
        bodyObject   <- parseResponse[A](httpResponse)(mf, formats)
      } yield bodyObject
    }

    private def doRequest(request: NdlaRequest): Try[Response[String]] = {
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

    private def parseResponse[A](response: Response[String])(implicit mf: Manifest[A], formats: Formats): Try[A] = {
      Try(parse(response.body).camelizeKeys.extract[A]) match {
        case Success(extracted) => Success(extracted)
        case Failure(ex)        =>
          // Large bodies in the error message can be very noisy.
          // If they are actually needed the `httpResponse` field of the exception can be used
          val errBody =
            if (response.body.length > ResponseErrorBodyCharacterCutoff)
              s"'${response.body.substring(0, 1000)}'... (Cut off)"
            else response.body

          val newEx = new HttpRequestException(s"Could not parse response with body: $errBody", Some(response))
          Failure(newEx.initCause(ex))
      }
    }

    private def addCorrelationId[T](request: NdlaRequest) = CorrelationID.get match {
      case None                => request
      case Some(correlationId) => request.header("X-Correlation-ID", correlationId)
    }

    private def addBasicAuth[T](request: NdlaRequest, user: String, password: String) = {
      request.auth.basic(user, password)
    }

    private def addForwardedAuth[T](request: NdlaRequest, tokenUser: Option[TokenUser]) = tokenUser match {
      case Some(TokenUser(_, _, _, Some(auth))) =>
        request.header(
          "Authorization",
          s"Bearer $auth",
          replaceExisting = true
        )
      case _ => request
    }
  }
}
