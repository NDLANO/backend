/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.model.api.Error
import no.ndla.network.ApplicationUrl
import no.ndla.network.model.HttpRequestException
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.{Ok, ScalatraServlet}

trait AudioApiController {
  val audioApiController: AudioApiController

  class AudioApiController extends ScalatraServlet with NativeJsonSupport with LazyLogging with CorrelationIdSupport {
    protected implicit override val jsonFormats: Formats = DefaultFormats

    before() {
      contentType = formats("json")
      ApplicationUrl.set(request)
    }

    after() {
      ApplicationUrl.clear()
    }

    error {
      case hre: HttpRequestException => halt(status = 502, body = Error(Error.REMOTE_ERROR, hre.getMessage))
      case t: Throwable => {
        t.printStackTrace()
        logger.error(t.getMessage)
        halt(status = 500, body = Error())
      }
    }

    get("/") {
      Ok(Seq())
    }
  }
}
