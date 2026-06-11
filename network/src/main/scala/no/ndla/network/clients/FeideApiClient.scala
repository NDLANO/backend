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
import no.ndla.network.model.{FeideAccessToken, HttpRequestException, NdlaRequest}
import no.ndla.common.model.domain.Availability
import no.ndla.common.errors.AccessDeniedException
import sttp.client4.Response
import sttp.client4.quick.*
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
    mail: Option[Seq[String]],
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
      Seq(Availability.everyone, Availability.teacher)
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

class FeideApiClient extends StrictLogging {

  private val feideTimeout          = 30.seconds
  private val feideUserInfoEndpoint = uri"https://api.dataporten.no/userinfo/v1/userinfo"
  private val feideGroupEndpoint    = uri"https://groups-api.dataporten.no/groups/me/groups"

  private def fetchFeideExtendedUser(accessToken: FeideAccessToken): Try[FeideExtendedUserInfo] =
    fetchAndParse[FeideExtendedUserInfo](accessToken, feideUserInfoEndpoint)
  private def fetchFeideGroupInfo(accessToken: FeideAccessToken): Try[Seq[FeideGroup]] =
    fetchAndParse[Seq[FeideGroup]](accessToken, feideGroupEndpoint)

  private def fetchAndParse[T: Decoder](accessToken: FeideAccessToken, endpoint: Uri): Try[T] = {
    val request = quickRequest.get(endpoint).readTimeout(feideTimeout).header("Authorization", s"Bearer $accessToken")

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
        Failure(HttpRequestException(s"Could not parse response ${response.body}", response))
    }
  }

  private def doRequest(request: NdlaRequest): Try[Response[String]] = {
    Try(request.send()).flatMap { response =>
      if (response.isSuccess) {
        Success(response)
      } else Failure(
        HttpRequestException(
          s"Received error ${response.code} ${response.statusText} when calling ${request.uri}. Body was ${response.body}",
          response,
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

  private def getFeideDataOrFail[T](feideResponse: Try[T]): Try[T] = {
    feideResponse match {
      case Failure(ex: HttpRequestException) if ex.code == 403 || ex.code == 401 || ex.code == 400 =>
        Failure(FeideApiClient.accessDeniedException)
      case Failure(ex)        => Failure(ex)
      case Success(feideData) => Success(feideData)
    }
  }

  def getFeideExtendedUser(accessToken: FeideAccessToken): Try[FeideExtendedUserInfo] =
    getFeideDataOrFail[FeideExtendedUserInfo](this.fetchFeideExtendedUser(accessToken))

  def getFeideGroupsAndOrganization(accessToken: FeideAccessToken): Try[(Seq[FeideGroup], String)] = for {
    groups       <- getFeideDataOrFail[Seq[FeideGroup]](this.fetchFeideGroupInfo(accessToken))
    organization <- findOrganization(groups)
  } yield (groups, organization)
}

object FeideApiClient {
  def accessDeniedException: AccessDeniedException = AccessDeniedException(
    "User could not be authenticated with feide and such is missing required role(s) to perform this operation"
  )
}
