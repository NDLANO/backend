/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import cats.effect.IO
import no.ndla.audioapi.Props
import no.ndla.audioapi.repository.AudioRepository
import no.ndla.network.tapir.{Service, TapirHealthController}
import org.http4s
import org.http4s.dsl.io._
import sttp.client3.Response
import sttp.client3.quick._

trait HealthController {
  this: AudioRepository with Props with Service with TapirHealthController =>
  val healthController: HealthController

  class HealthController extends TapirHealthController {
    private val localhost = "localhost"
    private val localport = props.ApplicationPort

    def getApiResponse(url: String): Response[String] = {
      simpleHttpClient.send(quickRequest.get(uri"$url"))
    }

    private def getReturnCode(imageResponse: Response[String]) = {
      imageResponse.code.code match {
        case 200 => Ok()
        case _   => InternalServerError()
      }
    }

    override def checkHealth(): IO[http4s.Response[IO]] = {
      audioRepository
        .getRandomAudio()
        .map(audio => {
          val id         = audio.id.get
          val previewUrl = s"http://$localhost:$localport${props.AudioControllerPath}$id"
          getReturnCode(getApiResponse(previewUrl))
        })
        .getOrElse(Ok())
    }
  }

}
