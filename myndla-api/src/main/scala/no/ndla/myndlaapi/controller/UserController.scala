/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.controller

import cats.implicits.*
import no.ndla.myndlaapi.MyNDLAAuthHelpers
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.Parameters.feideHeader
import no.ndla.network.tapir.TapirController
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import no.ndla.network.tapir.auth.Permission.LEARNINGPATH_API_ADMIN
import sttp.tapir.EndpointInput
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import io.circe.generic.auto.*
import no.ndla.common.model.api.myndla.{MyNDLAUserDTO, UpdatedMyNDLAUserDTO}
import no.ndla.common.model.domain.myndla.auth.AuthUtility
import no.ndla.myndlaapi.model.api.ExportedUserDataDTO
import no.ndla.myndlaapi.service.{ArenaReadService, FolderReadService, FolderWriteService, UserService}
import no.ndla.network.model.FeideID
import no.ndla.network.tapir.auth.TokenUser

trait UserController {
  this: ErrorHandling
    with UserService
    with MyNDLAAuthHelpers
    with FolderWriteService
    with FolderReadService
    with ArenaReadService
    with TapirController =>
  val userController: UserController

  class UserController extends TapirController {
    override val serviceName: String = "users"

    override protected val prefix: EndpointInput[Unit] = "myndla-api" / "v1" / serviceName

    def getMyNDLAUser: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Get user data")
      .description("Get user data")
      .in(feideHeader)
      .out(jsonBody[MyNDLAUserDTO])
      .errorOut(errorOutputsFor(401, 403, 404))
      .serverLogicPure { feideHeader =>
        userService.getMyNDLAUserData(feideHeader)
      }

    def updateMyNDLAUser: ServerEndpoint[Any, Eff] = endpoint.patch
      .summary("Update user data")
      .description("Update user data")
      .in(feideHeader)
      .in(jsonBody[UpdatedMyNDLAUserDTO])
      .out(jsonBody[MyNDLAUserDTO])
      .errorOut(errorOutputsFor(401, 403, 404))
      .serverLogicPure { case (feideHeader, updatedMyNdlaUser) =>
        userService.updateMyNDLAUserData(updatedMyNdlaUser, feideHeader)
      }

    def adminUpdateMyNDLAUser: ServerEndpoint[Any, Eff] = endpoint.patch
      .summary("Update some one elses user data")
      .description("Update some one elses user data")
      .in("update-other-user")
      .in(query[Option[FeideID]]("feide-id").description("FeideID of user"))
      .in(jsonBody[UpdatedMyNDLAUserDTO])
      .out(jsonBody[MyNDLAUserDTO])
      .errorOut(errorOutputsFor(401, 403, 404))
      .securityIn(TokenUser.oauth2Input(Seq.empty))
      .securityIn(AuthUtility.feideOauth())
      .serverSecurityLogicPure { case (tokenUser, feideToken) =>
        val arenaUser = feideToken.traverse(token => userService.getArenaEnabledUser(Some(token))).toOption.flatten
        if (tokenUser.hasPermission(LEARNINGPATH_API_ADMIN) || arenaUser.exists(_.isAdmin)) {
          Right((tokenUser, arenaUser))
        } else Left(ErrorHelpers.forbidden)
      }
      .serverLogicPure {
        case (tokenUser, myndlaUser) => { case (feideId, updatedMyNdlaUser) =>
          userService
            .adminUpdateMyNDLAUserData(updatedMyNdlaUser, feideId, tokenUser, myndlaUser)

        }
      }

    def deleteAllUserData: ServerEndpoint[Any, Eff] = endpoint.delete
      .summary("Delete all data connected to this user")
      .description("Delete all data connected to this user")
      .in("delete-personal-data")
      .in(feideHeader)
      .errorOut(errorOutputsFor(401, 403))
      .out(emptyOutput)
      .serverLogicPure { feideHeader =>
        arenaReadService.deleteAllUserData(feideHeader)
      }

    def exportUserData: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Export all stored user-related data as a json structure")
      .description("Export all stored user-related data as a json structure")
      .in("export")
      .in(feideHeader)
      .out(jsonBody[ExportedUserDataDTO])
      .errorOut(errorOutputsFor(401, 403))
      .serverLogicPure { feideHeader =>
        folderReadService.exportUserData(feideHeader)
      }

    def importUserData: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Import all stored user-related data from a exported json structure")
      .description("Import all stored user-related data from a exported json structure")
      .in("import")
      .in(feideHeader)
      .in(jsonBody[ExportedUserDataDTO])
      .out(jsonBody[ExportedUserDataDTO])
      .errorOut(errorOutputsFor(401, 403))
      .serverLogicPure { case (feideHeader, importBody) =>
        folderWriteService.importUserData(importBody, feideHeader)
      }

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      getMyNDLAUser,
      updateMyNDLAUser,
      adminUpdateMyNDLAUser,
      deleteAllUserData,
      exportUserData,
      importUserData
    )
  }
}
