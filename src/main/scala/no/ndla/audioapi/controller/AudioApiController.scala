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
import no.ndla.audioapi.model.api.{AudioMetaInformation, Error, ValidationException}
import no.ndla.audioapi.repository.AudioRepositoryComponent
import no.ndla.audioapi.service.ReadServiceComponent
import no.ndla.network.ApplicationUrl
import no.ndla.network.model.HttpRequestException
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.swagger.{Swagger, SwaggerSupport}
import org.scalatra._

trait AudioApiController {
  this: AudioRepositoryComponent with ReadServiceComponent =>
  val audioApiController: AudioApiController

  class AudioApiController(implicit val swagger: Swagger) extends ScalatraServlet with NativeJsonSupport with LazyLogging with SwaggerSupport with CorrelationIdSupport {
    protected implicit override val jsonFormats: Formats = DefaultFormats
    protected val applicationDescription = "API for accessing audio from ndla.no."

    val getByAudioId =
      (apiOperation[AudioMetaInformation]("findByAudioId")
        summary "Show audio info"
        notes "Shows info of the audio with submitted id."
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key. May be omitted to access api anonymously, but rate limiting may apply on anonymous access."),
        pathParam[String]("audio_id").description("Audio_id of the audio that needs to be fetched."))
        )

    before() {
      contentType = formats("json")
      ApplicationUrl.set(request)
      logger.info("{} {}{}", request.getMethod, request.getRequestURI, Option(request.getQueryString).map(s => s"?$s").getOrElse(""))
    }

    after() {
      ApplicationUrl.clear()
    }

    error {
      case v: ValidationException => BadRequest(Error(Error.VALIDATION, v.getMessage))
      case hre: HttpRequestException => BadGateway(Error(Error.REMOTE_ERROR, hre.getMessage))
      case t: Throwable => {
        t.printStackTrace()
        logger.error(t.getMessage)
        InternalServerError(Error())
      }
    }

    get("/") {
      Ok(Seq())
    }

    get("/:id", operation(getByAudioId)) {
      val id = long("id")
      readService.withId(id) match {
        case Some(audio) => audio
        case None => NotFound(Error(Error.NOT_FOUND, s"Audio with id $id not found"))
      }
    }

    private def long(paramName: String)(implicit request: HttpServletRequest): Long = {
      val paramValue = params(paramName)
      paramValue.forall(_.isDigit) match {
        case true => paramValue.toLong
        case false => throw new ValidationException(s"Invalid value for $paramName. Only digits are allowed.")
      }
    }

  }
}
