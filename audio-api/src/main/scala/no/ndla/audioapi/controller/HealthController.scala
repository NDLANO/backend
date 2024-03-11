/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.{Eff, Props}
import no.ndla.audioapi.repository.AudioRepository
import no.ndla.network.tapir.TapirHealthController
import sttp.client3.quick._

trait HealthController {
  this: AudioRepository with Props with TapirHealthController =>
  val healthController: HealthController

  class HealthController extends TapirHealthController[Eff] {
    private val localhost = "localhost"
    private val localport = props.ApplicationPort

    def getApiResponse(url: String): Int = {
      simpleHttpClient.send(quickRequest.get(uri"$url")).code.code
    }

    private def getReturnCode(imageResponse: Int) = {
      imageResponse match {
        case 200 => Right("Healthy")
        case _   => Left("Internal server error")
      }
    }

    override def checkHealth(): Either[String, String] = {
      audioRepository
        .getRandomAudio()
        .map(audio => {
          val id         = audio.id.get
          val previewUrl = s"http://$localhost:$localport${props.AudioControllerPath}$id"
          getReturnCode(getApiResponse(previewUrl))
        })
        .getOrElse(Right("Healthy"))
    }
  }

}
