/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.controller

import no.ndla.learningpathapi.model.api.ValidationError
import no.ndla.learningpathapi.service.{ConverterService, ReadService, UpdateService}
import org.json4s.ext.JavaTimeSerializers
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.swagger._
import org.scalatra.NoContent

import javax.servlet.http.HttpServletRequest
import no.ndla.scalatra.NdlaSwaggerSupport

trait UserController {
  this: ReadService with UpdateService with ConverterService with NdlaController =>
  val userController: UserController

  class UserController(implicit val swagger: Swagger) extends NdlaController with NdlaSwaggerSupport {

    protected implicit override val jsonFormats: Formats = DefaultFormats ++ JavaTimeSerializers.all

    protected val applicationDescription = "API for accessing My NDLA from ndla.no."

    // Additional models used in error responses
    registerModel[ValidationError]()
    registerModel[Error]()

    val response403: ResponseMessage = ResponseMessage(403, "Access not granted", Some("Error"))
    val response500: ResponseMessage = ResponseMessage(500, "Unknown error", Some("Error"))

    private val feideToken = Param[Option[String]]("FeideAuthorization", "Header containing FEIDE access token.")

    private def requestFeideToken(implicit request: HttpServletRequest): Option[String] = {
      request.header(this.feideToken.paramName).map(_.replaceFirst("Bearer ", ""))
    }

    delete(
      "/delete-personal-data/?",
      operation(
        apiOperation[Unit]("DeleteAllUserData")
          .summary("Delete all data connected to this user")
          .description("Delete all data connected to this user")
          .parameters(
            asHeaderParam(feideToken)
          )
          .responseMessages(response403, response500)
          .authorizations("oauth2")
      )
    ) {
      updateService.deleteAllUserData(requestFeideToken).map(_ => NoContent())
    }
  }
}
