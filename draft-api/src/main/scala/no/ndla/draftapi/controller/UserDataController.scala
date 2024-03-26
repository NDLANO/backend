/*
 * Part of NDLA draft-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.controller

import io.circe.generic.auto._
import no.ndla.draftapi.Eff
import no.ndla.draftapi.model.api.{ErrorHelpers, UpdatedUserData, UserData}
import no.ndla.draftapi.service.{ReadService, WriteService}
import no.ndla.network.tapir.NoNullJsonPrinter._
import no.ndla.network.tapir.Service
import no.ndla.network.tapir.TapirErrors.errorOutputsFor
import no.ndla.network.tapir.auth.Permission.DRAFT_API_WRITE
import sttp.tapir.{EndpointInput, _}
import sttp.tapir.generic.auto._
import sttp.tapir.server.ServerEndpoint

trait UserDataController {
  this: ReadService with WriteService with ErrorHelpers =>
  val userDataController: UserDataController

  class UserDataController extends Service[Eff] {
    import ErrorHelpers._
    override val serviceName: String         = "user-data"
    override val prefix: EndpointInput[Unit] = "draft-api" / "v1" / serviceName

    val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      getUserData,
      updateUserData
    )

    def getUserData: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Retrieves user's data")
      .description("Retrieves user's data")
      .out(jsonBody[UserData])
      .errorOut(errorOutputsFor(401, 403))
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure { userInfo => _ =>
        readService.getUserData(userInfo.id).handleErrorsOrOk
      }

    def updateUserData: ServerEndpoint[Any, Eff] = endpoint.patch
      .summary("Update data of logged in user")
      .description("Update data of logged in user")
      .in(jsonBody[UpdatedUserData])
      .out(jsonBody[UserData])
      .errorOut(errorOutputsFor(400, 401, 403))
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure { userInfo => updatedUserData =>
        writeService.updateUserData(updatedUserData, userInfo).handleErrorsOrOk
      }
  }

}
