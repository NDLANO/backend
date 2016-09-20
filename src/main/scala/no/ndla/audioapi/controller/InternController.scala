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
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.ScalatraServlet

trait InternController {
  val internController: InternController

  class InternController extends ScalatraServlet with NativeJsonSupport with LazyLogging with CorrelationIdSupport {

    protected implicit override val jsonFormats: Formats = DefaultFormats

    before() {
      contentType = formats("json")
      ApplicationUrl.set(request)
    }

    after() {
      ApplicationUrl.clear
    }

    error {
      case t: Throwable => {
        val error = Error(Error.GENERIC, t.getMessage)
        logger.error(error.toString, t)
        halt(status = 500, body = error)
      }
    }

  }
}
