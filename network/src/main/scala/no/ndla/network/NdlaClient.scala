/*
 * Part of NDLA network.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network

import no.ndla.network.model.HttpRequestException
import org.json4s.Formats
import org.json4s.jackson.JsonMethods._
import scalaj.http.{HttpRequest, HttpResponse}
import scala.util.{Failure, Success, Try}

trait NdlaClient {
  val ndlaClient: NdlaClient

  class NdlaClient {
    implicit val formats: Formats = org.json4s.DefaultFormats
    private val ResponseErrorBodyCharacterCutoff = 1000

    def fetch[A](request: HttpRequest)(implicit mf: Manifest[A]): Try[A] = {
      doFetch(addCorrelationId(request))
    }

    def fetchWithBasicAuth[A](request: HttpRequest, user: String, password: String)(
        implicit mf: Manifest[A],
        formats: Formats = formats): Try[A] = {
      doFetch(addCorrelationId(addBasicAuth(request, user, password)))
    }

    def fetchWithForwardedAuth[A](request: HttpRequest)(implicit mf: Manifest[A],
                                                        formats: Formats = formats): Try[A] = {
      doFetch(addCorrelationId(addForwardedAuth(request)))
    }

    /** Useful if response body is not json. */
    def fetchRawWithForwardedAuth(request: HttpRequest): Try[HttpResponse[String]] = {
      doRequest(addCorrelationId(addForwardedAuth(request)))
    }

    private def doFetch[A](request: HttpRequest)(implicit mf: Manifest[A], formats: Formats = formats): Try[A] = {
      for {
        httpResponse <- doRequest(request)
        bodyObject <- parseResponse[A](httpResponse)(mf, formats)
      } yield bodyObject
    }

    private def doRequest(request: HttpRequest): Try[HttpResponse[String]] = {
      Try(request.asString).flatMap(response => {
        response.isError match {
          case false => Success(response)
          case true => {
            Failure(new HttpRequestException(
              s"Received error ${response.code} ${response.statusLine} when calling ${request.url}. Body was ${response.body}",
              Some(response)
            ))
          }
        }
      })
    }

    private def parseResponse[A](response: HttpResponse[String])(implicit mf: Manifest[A],
                                                                 formats: Formats = formats): Try[A] = {
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

    private def addCorrelationId(request: HttpRequest) = CorrelationID.get match {
      case None                => request
      case Some(correlationId) => request.header("X-Correlation-ID", correlationId)
    }

    private def addBasicAuth(request: HttpRequest, user: String, password: String) = request.auth(user, password)

    private def addForwardedAuth(request: HttpRequest) = AuthUser.getHeader match {
      case Some(auth) => request.header("Authorization", auth)
      case None       => request
    }
  }
}
