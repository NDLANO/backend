/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.controller

import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.auth.FeideAuth
import no.ndla.network.tapir.TapirController
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import sttp.tapir.EndpointInput
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import no.ndla.common.model.api.myndla.{MyNDLAUserDTO, UpdatedMyNDLAUserDTO}
import no.ndla.myndlaapi.model.api.{ExportedUserDataDTO, FeideSessionDTO}
import no.ndla.myndlaapi.service.{FolderReadService, FolderWriteService, UserService}

class UserController(using
    userService: UserService,
    folderWriteService: FolderWriteService,
    folderReadService: FolderReadService,
    errorHandling: ControllerErrorHandling,
    feideAuth: FeideAuth,
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
    .serverLogicPure(feide => _ => Right(userService.getApiUserFromFeideWrapper(feide)))

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

  val setFeideSession: ServerEndpoint[Any, Eff] = endpoint
    .put
    .summary("Set Feide session data")
    .description("Set Feide ID and access token for a user session")
    .in("session")
    .in(jsonBody[FeideSessionDTO])
    .out(jsonBody[MyNDLAUserDTO])
    .errorOut(errorOutputsFor(401, 403))
    .withFeideIdToken
    .serverLogicPure(idToken => { case FeideSessionDTO(accessToken) =>
      userService.setFeideSessionAndGetUser(idToken, accessToken)
    })

  override val endpoints: List[ServerEndpoint[Any, Eff]] =
    List(getMyNDLAUser, updateMyNDLAUser, deleteAllUserData, exportUserData, importUserData, setFeideSession)
}
