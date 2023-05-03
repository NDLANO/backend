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
import no.ndla.common.Warmup
import no.ndla.network.tapir.Service
import org.http4s.HttpRoutes
import sttp.client3.Response
import sttp.client3.quick._
import org.http4s.dsl.io._

trait HealthController {
  this: AudioRepository with Props with Service =>
  val healthController: HealthController

  class HealthController extends Warmup with NoDocService {
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

    override def getBinding: (String, HttpRoutes[IO]) = "/health" -> {
      HttpRoutes.of[IO] { case GET -> Root =>
        if (!isWarmedUp) InternalServerError("Warmup hasn't finished")
        else {
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
  }

}
