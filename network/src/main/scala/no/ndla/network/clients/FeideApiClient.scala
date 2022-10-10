/*
 * Part of NDLA network.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.clients

import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.model.{FeideAccessToken, FeideID, HttpRequestException}
import no.ndla.common.model.domain.Availability
import no.ndla.common.errors.AccessDeniedException
import org.json4s.native.JsonMethods
import org.json4s.{DefaultFormats, Formats}
import scalaj.http.{Http, HttpRequest, HttpResponse}

import scala.util.{Failure, Success, Try}

case class FeideOpenIdUserInfo(sub: String)

case class FeideExtendedUserInfo(
    displayName: String,
    eduPersonAffiliation: Seq[String]
) {

  def isTeacher: Boolean = {
    this.eduPersonAffiliation.contains("staff") ||
    this.eduPersonAffiliation.contains("faculty") ||
    this.eduPersonAffiliation.contains("employee")
  }

  def availabilities: Seq[Availability.Value] = {
    if (this.isTeacher) {
      Seq(
        Availability.everyone,
        Availability.teacher
      )
    } else {
      Seq(Availability.everyone)
    }
  }
}

trait FeideApiClient {
  val feideApiClient: FeideApiClient

  class FeideApiClient extends LazyLogging {

    private val feideTimeout           = 1000 * 30
    private val openIdUserInfoEndpoint = "https://auth.dataporten.no/openid/userinfo"
    private val feideUserInfoEndpoint  = "https://api.dataporten.no/userinfo/v1/userinfo"

    private def getOpenIdUser(accessToken: FeideAccessToken): Try[FeideOpenIdUserInfo] =
      fetchAndParse[FeideOpenIdUserInfo](accessToken, openIdUserInfoEndpoint)
    def getUser(accessToken: FeideAccessToken): Try[FeideExtendedUserInfo] =
      fetchAndParse[FeideExtendedUserInfo](accessToken, feideUserInfoEndpoint)

    private def fetchAndParse[T](accessToken: FeideAccessToken, endpoint: String)(implicit mf: Manifest[T]): Try[T] = {
      val request =
        Http(endpoint)
          .timeout(feideTimeout, feideTimeout)
          .header("Authorization", s"Bearer $accessToken")

      implicit val formats: DefaultFormats.type = DefaultFormats
      for {
        response <- doRequest(request)
        parsed   <- parseResponse[T](response)
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

    def getFeideAccessTokenOrFail(maybeFeideAccessToken: Option[FeideAccessToken]): Try[FeideAccessToken] = {
      maybeFeideAccessToken match {
        case None =>
          Failure(
            AccessDeniedException("User is missing required role(s) to perform this operation", unauthorized = true)
          )
        case Some(feideAccessToken) => Success(feideAccessToken)
      }
    }

    def getUserFeideID(feideAccessToken: Option[FeideAccessToken]): Try[FeideID] = {
      getFeideAccessTokenOrFail(feideAccessToken).flatMap(accessToken =>
        this.getOpenIdUser(accessToken) match {
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
      )
    }

  }

}
