/*
 * Part of NDLA network.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network

import cats.effect.IO
import no.ndla.network.model.{HttpRequestException, NdlaRequest, RequestInfo}
import org.json4s.Formats
import org.json4s.jackson.JsonMethods._
import sttp.client3.{Response, SimpleHttpClient}
import sttp.client3.quick._

import scala.util.{Failure, Success, Try}

trait NdlaClient {
  val ndlaClient: NdlaClient

  class NdlaClient {
    val client: SimpleHttpClient                 = simpleHttpClient
    implicit val formats: Formats                = org.json4s.DefaultFormats
    private val ResponseErrorBodyCharacterCutoff = 1000

    def fetch[A](request: NdlaRequest)(implicit mf: Manifest[A]): IO[A] = for {
      withCorrelationId <- addCorrelationId(request)
      fetched           <- doFetch(withCorrelationId)
    } yield fetched

    def fetchWithBasicAuth[A](request: NdlaRequest, user: String, password: String)(implicit
        mf: Manifest[A],
        formats: Formats = formats
    ): IO[A] = {
      val withAuth = addBasicAuth(request, user, password)
      for {
        withCorrelationId <- addCorrelationId(withAuth)
        fetched           <- doFetch(withCorrelationId)
      } yield fetched
    }

    def fetchWithForwardedAuth[A](
        request: NdlaRequest
    )(implicit mf: Manifest[A], formats: Formats = formats): IO[A] = for {
      withAuth          <- addForwardedAuth(request)
      withCorrelationId <- addCorrelationId(withAuth)
      fetched           <- doFetch(withCorrelationId)
    } yield fetched

    /** Useful if response body is not json. */
    def fetchRawWithForwardedAuth(request: NdlaRequest): IO[Response[String]] = for {
      withAuth          <- addForwardedAuth(request)
      withCorrelationId <- addCorrelationId(withAuth)
      fetched           <- doRequest(withCorrelationId)
    } yield fetched

    private def doFetch[A](request: NdlaRequest)(implicit mf: Manifest[A], formats: Formats): IO[A] = {
      for {
        httpResponse <- doRequest(request)
        bodyObject   <- parseResponse[A](httpResponse)(mf, formats)
      } yield bodyObject
    }

    private def doRequest(request: NdlaRequest): IO[Response[String]] = {
      IO(client.send(request)).flatMap { response =>
        if (response.isSuccess) {
          IO.pure(response)
        } else {
          IO.raiseError(
            new HttpRequestException(
              s"Received error ${response.code} ${response.statusText} when calling ${request.uri}. Body was ${response.body}",
              Some(response)
            )
          )
        }
      }
    }

    private def parseResponse[A](response: Response[String])(implicit mf: Manifest[A], formats: Formats): IO[A] = {
      Try(parse(response.body).camelizeKeys.extract[A]) match {
        case Success(extracted) => IO.pure(extracted)
        case Failure(ex)        =>
          // Large bodies in the error message can be very noisy.
          // If they are actually needed the `httpResponse` field of the exception can be used
          val errBody =
            if (response.body.length > ResponseErrorBodyCharacterCutoff)
              s"'${response.body.substring(0, 1000)}'... (Cut off)"
            else response.body

          val newEx = new HttpRequestException(s"Could not parse response with body: $errBody", Some(response))
          IO.raiseError(newEx.initCause(ex))
      }
    }

    private def addCorrelationId[T](request: NdlaRequest): IO[NdlaRequest] = RequestInfo.get.map(r => {
      r.correlationId match {
        case None                => request
        case Some(correlationId) => request.header("X-Correlation-ID", correlationId)
      }
    })

    private def addBasicAuth(request: NdlaRequest, user: String, password: String): NdlaRequest = {
      request.auth.basic(user, password)
    }

    private def addForwardedAuth[T](request: NdlaRequest): IO[NdlaRequest] = {
      RequestInfo.get.map(r => {
        r.authUser.authHeader match {
          case Some(auth) => request.header("Authorization", auth, replaceExisting = true)
          case None       => request
        }
      })
    }
  }
}
