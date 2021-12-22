/*
 * Part of NDLA article-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.integration

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.model.domain.Availability
import no.ndla.articleapi.service.ConverterService
import no.ndla.network.NdlaClient
import no.ndla.network.model.HttpRequestException
import org.json4s.native.JsonMethods
import org.json4s.{DefaultFormats, Formats}
import scalaj.http.{Http, HttpRequest, HttpResponse}

import scala.util.{Failure, Success, Try}

case class FeideExtendedUserInfo(
    displayName: String,
    eduPersonAffiliation: Seq[String],
    eduPersonPrimaryAffiliation: String
) {
  def isStudent: Boolean = this.eduPersonAffiliation.contains("student")

  def isTeacher: Boolean = {
    this.eduPersonAffiliation.contains("staff") ||
    this.eduPersonAffiliation.contains("faculty") ||
    this.eduPersonAffiliation.contains("employee")
  }

  def availabilities: Seq[Availability.Value] = {
    if (this.isTeacher) {
      Seq(
        Availability.everyone,
        Availability.teacher,
        Availability.student
      )
    } else if (this.isStudent) {
      Seq(
        Availability.everyone,
        Availability.student
      )
    } else {
      Seq.empty
    }
  }
}

trait FeideApiClient {
  this: NdlaClient with ConverterService =>
  val feideApiClient: FeideApiClient

  class FeideApiClient extends LazyLogging {

    private val userInfoEndpoint = "https://api.dataporten.no/userinfo/v1/userinfo"

    private val feideTimeout = 1000 * 30

    def getUser(accessToken: String): Try[FeideExtendedUserInfo] = {
      val request =
        Http(userInfoEndpoint)
          .timeout(feideTimeout, feideTimeout)
          .header("Authorization", s"Bearer $accessToken")

      implicit val formats = DefaultFormats

      for {
        response <- doRequest(request)
        parsed <- parseResponse[FeideExtendedUserInfo](response)
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
          Failure(new HttpRequestException(
            s"Received error ${response.code} ${response.statusLine} when calling ${request.url}. Body was ${response.body}",
            Some(response)))
        } else {
          Success(response)
        }
      })
    }
  }

}
