/*
 * Part of NDLA draft-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.controller

import no.ndla.draftapi.model.api.{UpdatedUserData, UserData}
import no.ndla.draftapi.service.{ReadService, WriteService}
import no.ndla.network.tapir.auth.Permission.DRAFT_API_WRITE
import org.scalatra.Ok
import org.scalatra.swagger.{ResponseMessage, Swagger}

import scala.util.{Failure, Success}

trait UserDataController {
  this: ReadService with WriteService with NdlaController =>
  val userDataController: UserDataController

  class UserDataController(implicit val swagger: Swagger) extends NdlaController {
    protected val applicationDescription = "API for accessing user data."
    val response400: ResponseMessage     = ResponseMessage(400, "Validation Error", Some("ValidationError"))
    val response403: ResponseMessage     = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404: ResponseMessage     = ResponseMessage(404, "Not found", Some("Error"))
    val response500: ResponseMessage     = ResponseMessage(500, "Unknown error", Some("Error"))

    get(
      "/",
      operation(
        apiOperation[UserData]("getUserData")
          .summary("Retrieves user's data")
          .description("Retrieves user's data")
          .parameters(asHeaderParam(correlationId))
          .responseMessages(response403, response500)
          .authorizations(" oauth2")
      )
    ) {
      doOrAccessDeniedWithUser(DRAFT_API_WRITE) { userInfo =>
        readService.getUserData(userInfo.id) match {
          case Failure(error)    => errorHandler(error)
          case Success(userData) => userData
        }
      }
    }: Unit

    patch(
      "/",
      operation(
        apiOperation[UserData]("updateUserData")
          .summary("Update data of logged in user")
          .description("Update data of logged in user")
          .parameters(
            asHeaderParam[Option[String]](correlationId),
            bodyParam[UpdatedUserData]
          )
          .authorizations("oauth2")
          .responseMessages(response400, response403, response500)
      )
    ) {
      doOrAccessDeniedWithUser(DRAFT_API_WRITE) { userInfo =>
        val updatedUserData = tryExtract[UpdatedUserData](request.body)
        updatedUserData.flatMap(userData => writeService.updateUserData(userData, userInfo)) match {
          case Success(data)      => Ok(data)
          case Failure(exception) => errorHandler(exception)
        }
      }
    }: Unit
  }

}
