/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.integration

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.caching.Memoize
import no.ndla.learningpathapi.model.domain.{AccessDeniedException, FeideAccessToken, FeideID}
import no.ndla.network.NdlaClient
import no.ndla.network.model.HttpRequestException
import org.json4s.native.JsonMethods
import org.json4s.{DefaultFormats, Formats}
import scalaj.http.{Http, HttpRequest, HttpResponse}

import scala.util.{Failure, Success, Try}

case class FeideExtendedUserInfo(sub: String)

trait FeideApiClient {
  this: NdlaClient =>
  val feideApiClient: FeideApiClient

  class FeideApiClient extends LazyLogging {

    private val userInfoEndpoint = "https://auth.dataporten.no/openid/userinfo"
    private val feideTimeout     = 1000 * 30

    private def getUser(accessToken: String): Try[FeideExtendedUserInfo] = {
      val request =
        Http(userInfoEndpoint)
          .timeout(feideTimeout, feideTimeout)
          .header("Authorization", s"Bearer $accessToken")

      implicit val formats: DefaultFormats.type = DefaultFormats
      for {
        response <- doRequest(request)
        parsed   <- parseResponse[FeideExtendedUserInfo](response)
      } yield parsed
    }

    private def parseResponse[T](response: HttpResponse[String])(implicit mf: Manifest[T], formats: Formats): Try[T] = {
      Try(JsonMethods.parse(response.body).camelizeKeys.extract[T]) match {
        case Success(extracted) => Success(extracted)
        case Failure(ex) =>
          logger.error("Could not parse response from feide.", ex)
          Failure(new HttpRequestException(s"Could not parse response ${response.body}", Some(response)))
      }
    }

    private def doRequest(request: HttpRequest): Try[HttpResponse[String]] = {
      Try(request.asString).flatMap(response => {
        if (response.isError) {
          Failure(
            new HttpRequestException(
              s"Received error ${response.code} ${response.statusLine} when calling ${request.url}. Body was ${response.body}",
              Some(response)
            )
          )
        } else {
          Success(response)
        }
      })
    }

    def getUserFeideIDUncached(feideAccessToken: Option[FeideAccessToken]): Try[FeideID] = {
      feideAccessToken match {
        case None =>
          Failure(
            AccessDeniedException("User is missing required role(s) to perform this operation", unauthorized = true)
          )
        case Some(accessToken) =>
          this.getUser(accessToken) match {
            case Failure(ex: HttpRequestException) =>
              val code = ex.httpResponse.map(_.code)
              if (code.contains(403) || code.contains(401)) {
                Failure(
                  AccessDeniedException(
                    "User could not be authenticated with feide and such is missing required role(s) to perform this operation"
                  )
                )
              } else Failure(ex)
            case Failure(ex)        => Failure(ex)
            case Success(feideUser) => Success(feideUser.sub)
          }
      }
    }

    private val getUserFeideIDMemoize = Memoize(getUserFeideIDUncached)
    def getUserFeideID(feideAccessToken: Option[FeideAccessToken]): Try[FeideID] =
      getUserFeideIDMemoize(feideAccessToken)

  }

}
