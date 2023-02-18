/*
 * Part of NDLA network.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.clients

import com.typesafe.scalalogging.StrictLogging
import no.ndla.network.model.{FeideAccessToken, FeideID, HttpRequestException, NdlaRequest}
import no.ndla.common.model.domain.Availability
import no.ndla.common.errors.AccessDeniedException
import no.ndla.common.implicits.TryQuestionMark
import org.json4s.native.JsonMethods
import org.json4s.{DefaultFormats, Formats}
import sttp.client3.Response
import sttp.client3.quick._
import sttp.model.Uri

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

case class Membership(primarySchool: Option[Boolean])
case class FeideGroup(id: String, displayName: String, membership: Membership, parent: Option[String])

case class FeideOpenIdUserInfo(sub: String)

case class FeideExtendedUserInfo(
    displayName: String,
    eduPersonAffiliation: Seq[String],
    eduPersonPrincipalName: String
) {

  private def isStudentAffiliation: Boolean = this.eduPersonAffiliation.contains("student")
  private def isTeacherAffiliation: Boolean = {
    this.eduPersonAffiliation.contains("staff") ||
    this.eduPersonAffiliation.contains("faculty") ||
    this.eduPersonAffiliation.contains("employee")
  }

  def isTeacher: Boolean = {
    if (this.isStudentAffiliation) false
    else if (this.isTeacherAffiliation) true
    else false
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

  def email: String = this.eduPersonPrincipalName
}

trait FeideApiClient {
  this: RedisClient =>
  val feideApiClient: FeideApiClient

  class FeideApiClient extends StrictLogging {

    private val feideTimeout           = 30.seconds
    private val openIdUserInfoEndpoint = uri"https://auth.dataporten.no/openid/userinfo"
    private val feideUserInfoEndpoint  = uri"https://api.dataporten.no/userinfo/v1/userinfo"
    private val feideGroupEndpoint     = uri"https://groups-api.dataporten.no/groups/me/groups"

    private def fetchOpenIdUser(accessToken: FeideAccessToken): Try[FeideOpenIdUserInfo] =
      fetchAndParse[FeideOpenIdUserInfo](accessToken, openIdUserInfoEndpoint)
    private def fetchFeideExtendedUser(accessToken: FeideAccessToken): Try[FeideExtendedUserInfo] =
      fetchAndParse[FeideExtendedUserInfo](accessToken, feideUserInfoEndpoint)
    private def fetchFeideGroupInfo(accessToken: FeideAccessToken): Try[Seq[FeideGroup]] =
      fetchAndParse[Seq[FeideGroup]](accessToken, feideGroupEndpoint)

    private def fetchAndParse[T](accessToken: FeideAccessToken, endpoint: Uri)(implicit mf: Manifest[T]): Try[T] = {
      val request =
        quickRequest
          .get(endpoint)
          .readTimeout(feideTimeout)
          .header("Authorization", s"Bearer $accessToken")

      implicit val formats: DefaultFormats.type = DefaultFormats
      for {
        response <- doRequest(request)
        parsed   <- parseResponse[T](response)
      } yield parsed
    }

    private def parseResponse[T](response: Response[String])(implicit mf: Manifest[T], formats: Formats): Try[T] = {
      Try(JsonMethods.parse(response.body).camelizeKeys.extract[T]) match {
        case Success(extracted) => Success(extracted)
        case Failure(ex) =>
          logger.error("Could not parse response from feide.", ex)
          Failure(new HttpRequestException(s"Could not parse response ${response.body}", Some(response)))
      }
    }

    private def doRequest(request: NdlaRequest): Try[Response[String]] = {
      Try(simpleHttpClient.send(request)).flatMap { response =>
        if (response.isSuccess) {
          Success(response)
        } else
          Failure(
            new HttpRequestException(
              s"Received error ${response.code} ${response.statusText} when calling ${request.uri}. Body was ${response.body}",
              Some(response)
            )
          )
      }
    }

    private def findOrganization(feideGroups: Seq[FeideGroup]): Try[String] = {
      val primarySchoolGroup = feideGroups.find(group => group.membership.primarySchool.contains(true))
      val maybePrimaryGroup  = primarySchoolGroup.flatMap(e => feideGroups.find(group => e.parent.contains(group.id)))
      val fallback           = feideGroups.headOption
      maybePrimaryGroup.orElse(fallback) match {
        case Some(value) => Success(value.displayName)
        case None =>
          logger.error(
            "Can not determine organization. It is impossible to distinguish between the old and the current organization."
          )
          Failure(
            new NoSuchFieldException(
              "Can not determine organization. It is impossible to distinguish between the old and the current organization."
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
          if (code.exists(_.code == 403) || code.exists(_.code == 401)) {
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

    def getOrganization(feideAccessToken: Option[FeideAccessToken]): Try[String] = {
      val accessToken       = getFeideAccessTokenOrFail(feideAccessToken).?
      val maybeOrganization = redisClient.getOrganizationFromCache(accessToken).?
      val organization = (maybeOrganization match {
        case Some(organization) => Success(organization)
        case None =>
          getFeideDataOrFail[Seq[FeideGroup]](this.fetchFeideGroupInfo(accessToken)).flatMap(findOrganization)
      }).?
      redisClient.updateCacheAndReturnOrganization(accessToken, organization)
    }

  }

}
