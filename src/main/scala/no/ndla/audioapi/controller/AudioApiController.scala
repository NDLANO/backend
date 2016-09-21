/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.controller

import javax.servlet.http.HttpServletRequest

import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.model.api.{Error, ValidationException}
import no.ndla.audioapi.repository.AudioRepositoryComponent
import no.ndla.network.ApplicationUrl
import no.ndla.network.model.HttpRequestException
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.{NotFound, Ok, ScalatraServlet}

trait AudioApiController {
  this: AudioRepositoryComponent =>
  val audioApiController: AudioApiController

  class AudioApiController extends ScalatraServlet with NativeJsonSupport with LazyLogging with CorrelationIdSupport {
    protected implicit override val jsonFormats: Formats = DefaultFormats

    before() {
      contentType = formats("json")
      ApplicationUrl.set(request)
      logger.info("{} {}{}", request.getMethod, request.getRequestURI, Option(request.getQueryString).map(s => s"?$s").getOrElse(""))
    }

    after() {
      ApplicationUrl.clear()
    }

    error {
      case v: ValidationException => halt(status = 400, body = Error(Error.VALIDATION, v.getMessage))
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

    get("/:id") {
      val id = long("id")
      audioRepository.withId(id) match {
        case Some(audio) => audio
        case None => NotFound(Error(Error.NOT_FOUND, s"Audio with id $id not found"))
      }
    }

    def long(paramName: String)(implicit request: HttpServletRequest): Long = {
      val paramValue = params(paramName)
      paramValue.forall(_.isDigit) match {
        case true => paramValue.toLong
        case false => throw new ValidationException(s"Invalid value for $paramName. Only digits are allowed.")
      }
    }

  }
}
