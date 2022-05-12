/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import io.lemonlabs.uri.Uri.parse
import io.lemonlabs.uri.typesafe.dsl._
import no.ndla.imageapi.Props
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.network.ApplicationUrl
import org.scalatra._
import scalaj.http.{Http, HttpResponse}

trait HealthController {
  this: ImageRepository with Props =>
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
      val host = "localhost"
      val port = props.ApplicationPort

      imageRepository
        .getRandomImage()
        .map(image => {
          val previewUrl =
            s"http://$host:$port${props.RawControllerPath}/${parse(image.imageUrl.toStringRaw.dropWhile(_ == '/')).toString}"
          getReturnCode(getApiResponse(previewUrl))
        })
        .getOrElse(Ok())
    }
  }

}
