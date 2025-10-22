/*
 * Part of NDLA draft-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.controller

import no.ndla.draftapi.model.api.{UpdatedUserDataDTO, UserDataDTO}
import no.ndla.draftapi.service.{ReadService, WriteService}
import no.ndla.network.tapir.NoNullJsonPrinter.*
import no.ndla.network.tapir.{ErrorHandling, ErrorHelpers, TapirController}
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import no.ndla.network.tapir.auth.Permission.DRAFT_API_WRITE
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint

class UserDataController(using
    readService: ReadService,
    writeService: WriteService,
    errorHandling: ErrorHandling,
    errorHelpers: ErrorHelpers,
    myNDLAApiClient: MyNDLAApiClient,
) extends TapirController {
  override val serviceName: String         = "user-data"
  override val prefix: EndpointInput[Unit] = "draft-api" / "v1" / serviceName

  val endpoints: List[ServerEndpoint[Any, Eff]] = List(getUserData, updateUserData)

  def getUserData: ServerEndpoint[Any, Eff] = endpoint
    .get
    .summary("Retrieves user's data")
    .description("Retrieves user's data")
    .out(jsonBody[UserDataDTO])
    .errorOut(errorOutputsFor(401, 403))
    .requirePermission(DRAFT_API_WRITE)
    .serverLogicPure { userInfo => _ =>
      readService.getUserData(userInfo.id)
    }

  def updateUserData: ServerEndpoint[Any, Eff] = endpoint
    .patch
    .summary("Update data of logged in user")
    .description("Update data of logged in user")
    .in(jsonBody[UpdatedUserDataDTO])
    .out(jsonBody[UserDataDTO])
    .errorOut(errorOutputsFor(400, 401, 403))
    .requirePermission(DRAFT_API_WRITE)
    .serverLogicPure { userInfo => updatedUserData =>
      writeService.updateUserData(updatedUserData, userInfo)
    }
}
