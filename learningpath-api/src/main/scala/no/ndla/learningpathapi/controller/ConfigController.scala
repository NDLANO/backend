/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.controller

import no.ndla.learningpathapi.Props
import no.ndla.learningpathapi.model.api.ValidationError
import no.ndla.learningpathapi.model.api.config.{ConfigMeta, UpdateConfigValue}
import no.ndla.learningpathapi.model.domain.UserInfo
import no.ndla.learningpathapi.model.domain.config.ConfigKey
import no.ndla.learningpathapi.service.{ReadService, UpdateService}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.swagger.ResponseMessage

import scala.util.{Failure, Success}
import no.ndla.scalatra.NdlaSwaggerSupport
import org.json4s.ext.JavaTimeSerializers
import org.scalatra.swagger.Swagger
import org.scalatra.BadRequest

trait ConfigController {

  this: ReadService with UpdateService with NdlaController with Props with CorrelationIdSupport =>
  val configController: ConfigController

  class ConfigController(implicit val swagger: Swagger) extends NdlaController with NdlaSwaggerSupport {
    protected implicit override val jsonFormats: Formats = DefaultFormats ++ JavaTimeSerializers.all

    protected val applicationDescription =
      "API for changing configuration parameters for learningpaths from ndla.no."

    // Additional models used in error responses
    registerModel[ValidationError]()
    registerModel[Error]()

    val response400 = ResponseMessage(400, "Validation Error", Some("ValidationError"))
    val response403 = ResponseMessage(403, "Access not granted", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))
    val response502 = ResponseMessage(502, "Remote error", Some("Error"))

    private val correlationId =
      Param[Option[String]]("X-Correlation-ID", "User supplied correlation-id. May be omitted.")
    private val configKeyPathParam =
      Param[String](
        "config_key",
        s"""Key of configuration value. Can only be one of '${ConfigKey.values.mkString("', '")}'""".stripMargin
      )

    post(
      "/:config_key",
      operation(
        apiOperation[ConfigMeta]("updateConfig")
          .summary("Update configuration used by api.")
          .description("Update configuration used by api.")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(configKeyPathParam),
            bodyParam[UpdateConfigValue]
          )
          .responseMessages(response400, response404, response403, response500)
          .authorizations("oauth2")
      )
    ) {
      val userInfo        = UserInfo(requireUserId)
      val configKeyString = params("config_key")
      ConfigKey.valueOf(configKeyString) match {
        case None =>
          BadRequest(s"No such config key was found. Must be one of '${ConfigKey.values.mkString("', '")}'")
        case Some(configKey) =>
          val newConfigValue = extract[UpdateConfigValue](request.body)
          updateService.updateConfig(configKey, newConfigValue, userInfo) match {
            case Success(c)  => c
            case Failure(ex) => errorHandler(ex)
          }
      }
    }

  }
}
