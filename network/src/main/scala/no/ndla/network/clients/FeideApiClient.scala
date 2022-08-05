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
    eduPersonAffiliation: Seq[String],
    eduPersonPrimaryAffiliation: String
) {

  def isTeacher: Boolean = {
    this.eduPersonAffiliation.contains("staff") ||
    this.eduPersonAffiliation.contains("faculty") ||
    this.eduPersonAffiliation.contains("employee")
  }

  def availabilities: List[Availability.Value] = {
    if (this.isTeacher) {
      List(
        Availability.everyone,
        Availability.teacher
      )
    } else {
      List.empty
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
      getUser[FeideOpenIdUserInfo](accessToken, openIdUserInfoEndpoint)
    def getFeideUser(accessToken: FeideAccessToken): Try[FeideExtendedUserInfo] =
      getUser[FeideExtendedUserInfo](accessToken, feideUserInfoEndpoint)

    private def getUser[T](accessToken: FeideAccessToken, endpoint: String): Try[T] = {
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

    def getUserFeideIDUncached(feideAccessToken: Option[FeideAccessToken]): Try[FeideID] = {
      feideAccessToken match {
        case None =>
          Failure(
            AccessDeniedException("User is missing required role(s) to perform this operation", unauthorized = true)
          )
        case Some(accessToken) =>
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
      }
    }

  }

}
