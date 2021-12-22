/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.AudioApiProperties
import no.ndla.audioapi.repository.AudioRepository
import no.ndla.network.ApplicationUrl
import org.scalatra.{ActionResult, InternalServerError, Ok, ScalatraServlet}

import scalaj.http.{Http, HttpResponse}

trait HealthController {
  this: AudioRepository =>
  val healthController: HealthController

  class HealthController extends ScalatraServlet {

    before() {
      ApplicationUrl.set(request)
    }

    after() {
      ApplicationUrl.clear()
    }

    def getApiResponse(url: String): HttpResponse[String] = {
      Http(url).execute()
    }

    def getReturnCode(imageResponse: HttpResponse[String]): ActionResult = {
      imageResponse.code match {
        case 200 => Ok()
        case _   => InternalServerError()
      }
    }

    get("/") {
      val applicationUrl = ApplicationUrl.get
      val host = "localhost"
      val port = AudioApiProperties.ApplicationPort

      audioRepository
        .getRandomAudio()
        .map(audio => {
          val id = audio.id.get
          val previewUrl = s"http://$host:$port${AudioApiProperties.AudioControllerPath}$id"
          getReturnCode(getApiResponse(previewUrl))
        })
        .getOrElse(Ok())
    }
  }

}
