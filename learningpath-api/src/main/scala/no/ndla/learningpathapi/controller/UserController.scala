/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.controller

import no.ndla.common.scalatra.NdlaSwaggerSupport
import no.ndla.learningpathapi.model.api.{ExportedUserData, FeideUser, UpdatedFeideUser, ValidationError}
import no.ndla.learningpathapi.service.{ConverterService, ReadService, UpdateService}
import org.json4s.ext.JavaTimeSerializers
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.swagger._
import org.scalatra.NoContent

import javax.servlet.http.HttpServletRequest

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
    val response404: ResponseMessage = ResponseMessage(404, "Not Found", Some("Error"))
    val response500: ResponseMessage = ResponseMessage(500, "Unknown error", Some("Error"))

    private val feideToken = Param[Option[String]]("FeideAuthorization", "Header containing FEIDE access token.")

    private def requestFeideToken(implicit request: HttpServletRequest): Option[String] = {
      request.header(this.feideToken.paramName).map(_.replaceFirst("Bearer ", ""))
    }

    get(
      "/",
      operation(
        apiOperation[FeideUser]("GetFeideUser")
          .summary("Get user data")
          .description("Get user data")
          .parameters(
            asHeaderParam(feideToken)
          )
          .responseMessages(response403, response500)
          .authorizations("oauth2")
      )
    ) {
      readService.getFeideUserData(requestFeideToken)
    }

    patch(
      "/",
      operation(
        apiOperation[FeideUser]("UpdateFeideUser")
          .summary("Update user data")
          .description("Update user data")
          .parameters(
            asHeaderParam(feideToken),
            bodyParam[UpdatedFeideUser]
          )
          .responseMessages(response403, response404, response500)
          .authorizations("oauth2")
      )
    ) {
      val updatedUserData = extract[UpdatedFeideUser](request.body)
      updateService.updateFeideUserData(updatedUserData, requestFeideToken)
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

    get(
      "/export",
      operation(
        apiOperation[ExportedUserData]("exportUserData")
          .summary("Export all stored user-related data as a json structure")
          .description("Export all stored user-related data as a json structure")
          .parameters(asHeaderParam(feideToken))
      )
    ) {
      readService.exportUserData(requestFeideToken)
    }
  }
}
