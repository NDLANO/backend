/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.AudioApiProperties
import no.ndla.audioapi.model.api.{AudioSummary, SearchResult}
import org.json4s.DefaultFormats
import org.json4s.native.JsonParser
import org.scalatra.{ActionResult, InternalServerError, Ok, ScalatraServlet}

import scalaj.http.{Http, HttpResponse}

trait HealthController {
  val healthController: HealthController

  class HealthController extends ScalatraServlet {

    implicit val formats = DefaultFormats

    def getApiResponse(url: String): HttpResponse[String] = {
      Http(url).execute()
    }

    def getAudioUrl(body: String): (Option[String], Long) = {
      val json = JsonParser.parse(body).extract[SearchResult]
      json.results.headOption match {
        case Some(result: AudioSummary) => (Some(result.url), json.totalCount)
        case _ => (None,json.totalCount)
      }
    }

    def getReturnCode(imageResponse: HttpResponse[String]): ActionResult = {
      imageResponse.code match {
        case 200 => Ok()
        case _ => InternalServerError()
      }
    }

    get("/") {
      val apiSearchResponse = getApiResponse(
        s"http://0.0.0.0:${AudioApiProperties.ApplicationPort}${AudioApiProperties.AudioControllerPath}")
      val (audioUrl,totalCount) = getAudioUrl(apiSearchResponse.body)

      apiSearchResponse.code match {
        case _ if totalCount == 0 => Ok()
        case 200 => getReturnCode(getApiResponse(audioUrl.get))
        case _ => InternalServerError()
      }
    }
  }
}
