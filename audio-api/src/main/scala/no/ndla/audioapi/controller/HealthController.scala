/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.Props
import no.ndla.audioapi.repository.AudioRepository
import no.ndla.network.scalatra.BaseHealthController
import org.scalatra.{ActionResult, InternalServerError, Ok}
import sttp.client3.Response
import sttp.client3.quick._

trait HealthController {
  this: AudioRepository with Props =>
  val healthController: HealthController

  class HealthController extends BaseHealthController {

    def getApiResponse(url: String): Response[String] = {
      simpleHttpClient.send(quickRequest.get(uri"$url"))
    }

    def getReturnCode(imageResponse: Response[String]): ActionResult = {
      imageResponse.code.code match {
        case 200 => Ok()
        case _   => InternalServerError()
      }
    }

    get("/") {
      val host = "localhost"
      val port = props.ApplicationPort

      audioRepository
        .getRandomAudio()
        .map(audio => {
          val id         = audio.id.get
          val previewUrl = s"http://$host:$port${props.AudioControllerPath}$id"
          getReturnCode(getApiResponse(previewUrl))
        })
        .getOrElse(Ok())
    }
  }

}
