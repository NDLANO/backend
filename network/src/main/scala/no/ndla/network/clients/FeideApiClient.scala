/*
 * Part of NDLA network
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.clients

import com.typesafe.scalalogging.StrictLogging
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.CirceUtil
import no.ndla.network.model.{FeideAccessToken, FeideID, HttpRequestException, NdlaRequest}
import no.ndla.common.model.domain.Availability
import no.ndla.common.errors.AccessDeniedException
import no.ndla.common.implicits.*
import sttp.client3.Response
import sttp.client3.quick.*
import sttp.model.Uri

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

case class Membership(primarySchool: Option[Boolean])

object Membership {
  implicit val encoder: Encoder[Membership] = deriveEncoder
  implicit val decoder: Decoder[Membership] = deriveDecoder
}
case class FeideGroup(id: String, `type`: String, displayName: String, membership: Membership, parent: Option[String])
object FeideGroup {
  val FC_ORG = "fc:org"

  implicit val encoder: Encoder[FeideGroup] = deriveEncoder
  implicit val decoder: Decoder[FeideGroup] = deriveDecoder
}

case class FeideOpenIdUserInfo(sub: String)

object FeideOpenIdUserInfo {
  implicit val encoder: Encoder[FeideOpenIdUserInfo] = deriveEncoder
  implicit val decoder: Decoder[FeideOpenIdUserInfo] = deriveDecoder
}

case class FeideExtendedUserInfo(
    displayName: String,
    eduPersonAffiliation: Seq[String],
    eduPersonPrimaryAffiliation: Option[String],
    eduPersonPrincipalName: String,
    mail: Option[Seq[String]]
) {

  private def isTeacherAffiliation: Boolean = {
    !this.eduPersonPrimaryAffiliation.contains("student") &&
    (this.eduPersonAffiliation.contains("staff") ||
      this.eduPersonAffiliation.contains("faculty") ||
      this.eduPersonAffiliation.contains("employee"))
  }

  def isTeacher: Boolean = {
    if (this.isTeacherAffiliation) true
    else false
  }

  def availabilities: Seq[Availability] = {
    if (this.isTeacher) {
      Seq(
        Availability.everyone,
        Availability.teacher
      )
    } else {
      Seq(Availability.everyone)
    }
  }

  def email: String    = this.mail.getOrElse(Seq(this.eduPersonPrincipalName)).head
  def username: String = this.eduPersonPrincipalName
}

object FeideExtendedUserInfo {
  implicit val decoder: Decoder[FeideExtendedUserInfo] = deriveDecoder
  implicit val encoder: Encoder[FeideExtendedUserInfo] = deriveEncoder
}

class FeideApiClient(using redisClient: RedisClient) extends StrictLogging {

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

  private def fetchAndParse[T: Decoder](accessToken: FeideAccessToken, endpoint: Uri): Try[T] = {
    val request =
      quickRequest
        .get(endpoint)
        .readTimeout(feideTimeout)
        .header("Authorization", s"Bearer $accessToken")

    for {
      response <- doRequest(request)
      parsed   <- parseResponse[T](response)
    } yield parsed
  }

  private def parseResponse[T: Decoder](response: Response[String]): Try[T] = {
    CirceUtil.tryParseAs[T](response.body) match {
      case Success(extracted) => Success(extracted)
      case Failure(ex)        =>
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
      case None        =>
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
        if (code.exists(_.code == 403) || code.exists(_.code == 401) || code.exists(_.code == 400)) {
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
      accessToken   <- getFeideAccessTokenOrFail(feideAccessToken)
      maybeFeideId  <- redisClient.getFeideIdFromCache(accessToken)
      feideOpenUser <- maybeFeideId match {
        case Some(feideId) => Success(FeideOpenIdUserInfo(feideId))
        case None          => getFeideDataOrFail[FeideOpenIdUserInfo](this.fetchOpenIdUser(accessToken))
      }
      feideId <- redisClient.updateCacheAndReturnFeideId(accessToken, feideOpenUser.sub)
    } yield feideId
  }

  def getFeideExtendedUser(feideAccessToken: Option[FeideAccessToken]): Try[FeideExtendedUserInfo] = permitTry {
    val accessToken       = getFeideAccessTokenOrFail(feideAccessToken).?
    val maybeFeideUser    = redisClient.getFeideUserFromCache(accessToken).?
    val feideExtendedUser = (maybeFeideUser match {
      case Some(feideUser) => Success(feideUser)
      case None            => getFeideDataOrFail[FeideExtendedUserInfo](this.fetchFeideExtendedUser(accessToken))
    }).?
    redisClient.updateCacheAndReturnFeideUser(accessToken, feideExtendedUser)
  }

  def getFeideGroups(feideAccessToken: Option[FeideAccessToken]): Try[Seq[FeideGroup]] = permitTry {
    val accessToken      = getFeideAccessTokenOrFail(feideAccessToken).?
    val maybeFeideGroups = redisClient.getGroupsFromCache(accessToken).?
    val feideGroups      = (maybeFeideGroups match {
      case Some(groups) => Success(groups)
      case None         => getFeideDataOrFail[Seq[FeideGroup]](this.fetchFeideGroupInfo(accessToken))
    }).?
    redisClient.updateCacheAndReturnGroups(accessToken, feideGroups)
  }

  def getOrganization(feideAccessToken: Option[FeideAccessToken]): Try[String] = permitTry {
    val accessToken       = getFeideAccessTokenOrFail(feideAccessToken).?
    val maybeOrganization = redisClient.getOrganizationFromCache(accessToken).?
    val organization      = (maybeOrganization match {
      case Some(organization) => Success(organization)
      case None               =>
        getFeideDataOrFail[Seq[FeideGroup]](this.fetchFeideGroupInfo(accessToken)).flatMap(findOrganization)
    }).?
    redisClient.updateCacheAndReturnOrganization(accessToken, organization)
  }

}
