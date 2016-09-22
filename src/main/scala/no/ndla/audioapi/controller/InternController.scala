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
import no.ndla.audioapi.service.{ConverterService, ImportServiceComponent}
import no.ndla.network.ApplicationUrl
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.{InternalServerError, ScalatraServlet}

import scala.util.{Failure, Success}

trait InternController {
  this: ImportServiceComponent with ConverterService =>
  val internController: InternController

  class InternController extends ScalatraServlet with NativeJsonSupport with LazyLogging with CorrelationIdSupport {

    protected implicit override val jsonFormats: Formats = DefaultFormats

    before() {
      contentType = formats("json")
      ApplicationUrl.set(request)
      logger.info("{} {}{}", request.getMethod, request.getRequestURI, Option(request.getQueryString).map(s => s"?$s").getOrElse(""))
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

    post("/import/:external_id") {
      val externalId = params("external_id")
      importService.importAudio(externalId) match {
        case Success(audio) => converterService.toApiAudioMetaInformation(audio)
        case Failure(ex) => {
          val errorMessage = s"Import of audio with external_id $externalId failed: ${ex.getMessage}"
          logger.warn(errorMessage, ex)
          InternalServerError(errorMessage)
        }
      }
    }

  }
}
