/*
 * Part of NDLA network.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.clients

import com.typesafe.scalalogging.StrictLogging
import no.ndla.network.model.{FeideAccessToken, FeideID, HttpRequestException}
import no.ndla.common.model.domain.Availability
import no.ndla.common.errors.AccessDeniedException
import no.ndla.common.implicits.TryQuestionMark
import org.json4s.native.JsonMethods
import org.json4s.{DefaultFormats, Formats}
import scalaj.http.{Http, HttpRequest, HttpResponse}

import scala.util.{Failure, Success, Try}

case class Membership(primaryAffiliation: Option[String])
case class FeideGroup(displayName: String, membership: Membership)

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
  this: RedisClient =>
  val feideApiClient: FeideApiClient

  class FeideApiClient extends StrictLogging {

    private val feideTimeout           = 1000 * 30
    private val openIdUserInfoEndpoint = "https://auth.dataporten.no/openid/userinfo"
    private val feideUserInfoEndpoint  = "https://api.dataporten.no/userinfo/v1/userinfo"
    private val feideGroupEndpoint     = "https://groups-api.dataporten.no/groups/me/groups"

    private def fetchOpenIdUser(accessToken: FeideAccessToken): Try[FeideOpenIdUserInfo] =
      fetchAndParse[FeideOpenIdUserInfo](accessToken, openIdUserInfoEndpoint)
    private def fetchFeideExtendedUser(accessToken: FeideAccessToken): Try[FeideExtendedUserInfo] =
      fetchAndParse[FeideExtendedUserInfo](accessToken, feideUserInfoEndpoint)
    private def fetchFeideGroupInfo(accessToken: FeideAccessToken): Try[Seq[FeideGroup]] =
      fetchAndParse[Seq[FeideGroup]](accessToken, feideGroupEndpoint)

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

    private def findCounty(feideGroups: Seq[FeideGroup]): Try[String] = {
      feideGroups.find(group => group.membership.primaryAffiliation.isDefined) match {
        case Some(value) => Success(value.displayName)
        case None =>
          logger.error(
            "None of feideGroup list contained 'primaryAffiliation' so it is impossible to distinguish between old organisation and the current one."
          )
          Failure(
            new NoSuchFieldException(
              "None of feideGroup list contained 'primaryAffiliation' so it is impossible to distinguish between old organisation and the current one."
            )
          )
      }
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

    private def getFeideDataOrFail[T](feideResponse: Try[T]): Try[T] = {
      feideResponse match {
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
        case Success(feideData) => Success(feideData)
      }
    }

    def getFeideID(feideAccessToken: Option[FeideAccessToken]): Try[FeideID] = {
      for {
        accessToken  <- getFeideAccessTokenOrFail(feideAccessToken)
        maybeFeideId <- redisClient.getFeideIdFromCache(accessToken)
        feideOpenUser <- maybeFeideId match {
          case Some(feideId) => Success(FeideOpenIdUserInfo(feideId))
          case None          => getFeideDataOrFail[FeideOpenIdUserInfo](this.fetchOpenIdUser(accessToken))
        }
        feideId <- redisClient.updateCacheAndReturnFeideId(accessToken, feideOpenUser.sub)
      } yield feideId
    }

    def getFeideExtendedUser(feideAccessToken: Option[FeideAccessToken]): Try[FeideExtendedUserInfo] = {
      val accessToken    = getFeideAccessTokenOrFail(feideAccessToken).?
      val maybeFeideUser = redisClient.getFeideUserFromCache(accessToken).?
      val feideExtendedUser = (maybeFeideUser match {
        case Some(feideUser) => Success(feideUser)
        case None            => getFeideDataOrFail[FeideExtendedUserInfo](this.fetchFeideExtendedUser(accessToken))
      }).?
      redisClient.updateCacheAndReturnFeideUser(accessToken, feideExtendedUser)
    }

    def getCounty(feideAccessToken: Option[FeideAccessToken]): Try[String] = {
      val accessToken = getFeideAccessTokenOrFail(feideAccessToken).?
      val maybeCounty = redisClient.getCountyFromCache(accessToken).?
      val county = (maybeCounty match {
        case Some(county) => Success(county)
        case None => getFeideDataOrFail[Seq[FeideGroup]](this.fetchFeideGroupInfo(accessToken)).flatMap(findCounty)
      }).?
      redisClient.updateCacheAndReturnCounty(accessToken, county)
    }

  }

}
