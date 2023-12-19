/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.controller

import no.ndla.common.model.NDLADate
import no.ndla.learningpathapi.model.api.{Error, ValidationError}
import no.ndla.learningpathapi.service.{ConverterService, ReadService, UpdateService}
import no.ndla.myndla.model.api.{ExportedUserData, MyNDLAUser, UpdatedMyNDLAUser}
import no.ndla.myndla.service.{FolderReadService, FolderWriteService, UserService}
import no.ndla.network.tapir.auth.Permission.LEARNINGPATH_API_ADMIN
import org.json4s.ext.JavaTimeSerializers
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.NoContent
import org.scalatra.swagger._

import javax.servlet.http.HttpServletRequest

trait UserController {
  this: ReadService
    with UpdateService
    with ConverterService
    with NdlaController
    with UserService
    with FolderWriteService
    with FolderReadService =>
  val userController: UserController

  class UserController(implicit val swagger: Swagger) extends NdlaController {

    protected implicit override val jsonFormats: Formats =
      DefaultFormats ++
        JavaTimeSerializers.all +
        NDLADate.Json4sSerializer

    protected val applicationDescription = "API for accessing My NDLA from ndla.no."

    // Additional models used in error responses
    registerModel[ValidationError]()
    registerModel[Error]()

    val response403: ResponseMessage = ResponseMessage(403, "Access not granted", Some("Error"))
    val response404: ResponseMessage = ResponseMessage(404, "Not Found", Some("Error"))
    val response500: ResponseMessage = ResponseMessage(500, "Unknown error", Some("Error"))

    private val feideToken = Param[Option[String]]("FeideAuthorization", "Header containing FEIDE access token.")
    private val feideId    = Param[Option[String]]("feide-id", "FeideID of user")

    private def requestFeideToken(implicit request: HttpServletRequest): Option[String] = {
      request.header(this.feideToken.paramName).map(_.replaceFirst("Bearer ", ""))
    }

    get(
      "/",
      operation(
        apiOperation[MyNDLAUser]("GetMyNDLAUser")
          .summary("Get user data")
          .description("Get user data")
          .parameters(
            asHeaderParam(feideToken)
          )
          .responseMessages(response403, response500)
          .authorizations("oauth2")
      )
    ) {
      userService.getMyNDLAUserData(requestFeideToken)
    }: Unit

    patch(
      "/",
      operation(
        apiOperation[MyNDLAUser]("UpdateMyNDLAUser")
          .summary("Update user data")
          .description("Update user data")
          .parameters(
            asHeaderParam(feideToken),
            bodyParam[UpdatedMyNDLAUser]
          )
          .responseMessages(response403, response404, response500)
          .authorizations("oauth2")
      )
    ) {
      val updatedUserData = extract[UpdatedMyNDLAUser](request.body)
      userService.updateMyNDLAUserData(updatedUserData, requestFeideToken)
    }: Unit

    patch(
      "/update-other-user/?",
      operation(
        apiOperation[MyNDLAUser]("AdminUpdateMyNDLAUser")
          .summary("Update some one elses user data")
          .description("Update some one elses user data")
          .parameters(
            asQueryParam(feideId),
            bodyParam[UpdatedMyNDLAUser]
          )
          .responseMessages(response403, response404, response500)
          .authorizations("oauth2")
      )
    ) {
      requirePermissionOrAccessDeniedWithUser(LEARNINGPATH_API_ADMIN) { user =>
        val updatedUserData = extract[UpdatedMyNDLAUser](request.body)
        val feideId         = paramOrNone(this.feideId.paramName)
        userService.adminUpdateMyNDLAUserData(updatedUserData, feideId, Some(user), None)
      }
    }: Unit

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
      folderWriteService.deleteAllUserData(requestFeideToken).map(_ => NoContent())
    }: Unit

    get(
      "/export",
      operation(
        apiOperation[ExportedUserData]("exportUserData")
          .summary("Export all stored user-related data as a json structure")
          .description("Export all stored user-related data as a json structure")
          .parameters(asHeaderParam(feideToken))
      )
    ) {
      folderReadService.exportUserData(requestFeideToken)
    }: Unit

    post(
      "/import",
      operation(
        apiOperation[ExportedUserData]("importUserData")
          .summary("Import all stored user-related data from a exported json structure")
          .description("Import all stored user-related data from a exported json structure")
          .parameters(
            asHeaderParam(feideToken),
            bodyParam[ExportedUserData]
          )
      )
    ) {
      val importBody = tryExtract[ExportedUserData](request.body)
      importBody.flatMap(importBody => folderWriteService.importUserData(importBody, requestFeideToken))
    }: Unit
  }
}
