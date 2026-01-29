/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.controller

import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.{ErrorHelpers, TapirController}
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import sttp.tapir.EndpointInput
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import no.ndla.common.model.api.myndla.{MyNDLAUserDTO, UpdatedMyNDLAUserDTO}
import no.ndla.myndlaapi.model.api.ExportedUserDataDTO
import no.ndla.myndlaapi.service.{FolderReadService, FolderWriteService, UserService}
import no.ndla.myndlaapi.integration.InternalMyNDLAApiClient

class UserController(using
    userService: UserService,
    folderWriteService: FolderWriteService,
    folderReadService: FolderReadService,
    errorHandling: ControllerErrorHandling,
    errorHelpers: ErrorHelpers,
    myNDLAApiClient: InternalMyNDLAApiClient,
) extends TapirController {
  override val serviceName: String = "users"

  override protected val prefix: EndpointInput[Unit] = "myndla-api" / "v1" / serviceName

  def getMyNDLAUser: ServerEndpoint[Any, Eff] = endpoint
    .get
    .summary("Get user data")
    .description("Get user data")
    .out(jsonBody[MyNDLAUserDTO])
    .errorOut(errorOutputsFor(401, 403, 404))
    .withFeideUser
    .serverLogicPure(feide => _ => userService.getMyNDLAUserData(Some(feide.token)))

  def updateMyNDLAUser: ServerEndpoint[Any, Eff] = endpoint
    .patch
    .summary("Update user data")
    .description("Update user data")
    .in(jsonBody[UpdatedMyNDLAUserDTO])
    .out(jsonBody[MyNDLAUserDTO])
    .errorOut(errorOutputsFor(401, 403, 404))
    .withFeideUser
    .serverLogicPure { feide =>
      { updatedMyNdlaUser =>
        userService.updateMyNDLAUserData(updatedMyNdlaUser, feide)
      }
    }

  def deleteAllUserData: ServerEndpoint[Any, Eff] = endpoint
    .delete
    .summary("Delete all data connected to this user")
    .description("Delete all data connected to this user")
    .in("delete-personal-data")
    .errorOut(errorOutputsFor(401, 403))
    .out(noContent)
    .withFeideUser
    .serverLogicPure(feide => _ => userService.deleteAllUserData(feide))

  def exportUserData: ServerEndpoint[Any, Eff] = endpoint
    .get
    .summary("Export all stored user-related data as a json structure")
    .description("Export all stored user-related data as a json structure")
    .in("export")
    .out(jsonBody[ExportedUserDataDTO])
    .errorOut(errorOutputsFor(401, 403))
    .withFeideUser
    .serverLogicPure(feide => _ => folderReadService.exportUserData(feide))

  def importUserData: ServerEndpoint[Any, Eff] = endpoint
    .post
    .summary("Import all stored user-related data from a exported json structure")
    .description("Import all stored user-related data from a exported json structure")
    .in("import")
    .in(jsonBody[ExportedUserDataDTO])
    .out(jsonBody[ExportedUserDataDTO])
    .errorOut(errorOutputsFor(401, 403))
    .withFeideUser
    .serverLogicPure { feide => importBody =>
      folderWriteService.importUserData(importBody, feide)
    }

  override val endpoints: List[ServerEndpoint[Any, Eff]] =
    List(getMyNDLAUser, updateMyNDLAUser, deleteAllUserData, exportUserData, importUserData)
}
