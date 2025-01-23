/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.service

import no.ndla.common.Clock
import no.ndla.common.errors.{AccessDeniedException, NotFoundException, ValidationException}
import no.ndla.common.implicits.TryQuestionMark
import no.ndla.common.model.api.myndla
import no.ndla.common.model.api.myndla.UpdatedMyNDLAUserDTO
import no.ndla.common.model.domain.myndla.{ArenaGroup, MyNDLAGroup, MyNDLAUser, MyNDLAUserDocument, UserRole}
import no.ndla.myndlaapi.model.api.{ArenaUserDTO, PaginatedArenaUsersDTO}
import no.ndla.myndlaapi.repository.UserRepository
import no.ndla.network.clients.{FeideApiClient, FeideGroup}
import no.ndla.network.model.{FeideAccessToken, FeideID}
import no.ndla.network.tapir.auth.TokenUser
import scalikejdbc.{AutoSession, DBSession, ReadOnlyAutoSession}

import scala.annotation.unused
import scala.util.{Failure, Success, Try}

trait UserService {
  this: FeideApiClient & FolderConverterService & ConfigService & UserRepository & Clock & FolderWriteService =>

  val userService: UserService

  class UserService {
    def getArenaUserByUserName(username: String): Try[ArenaUserDTO] = {
      userRepository.userWithUsername(username) match {
        case Failure(ex)         => Failure(ex)
        case Success(Some(user)) => Success(ArenaUserDTO.from(user))
        case Success(None)       => Failure(NotFoundException(s"User with username '$username' was not found"))
      }
    }

    private def getUserById(userId: Long)(session: DBSession): Try[MyNDLAUser] = {
      userRepository.userWithId(userId)(session) match {
        case Failure(ex)         => Failure(ex)
        case Success(Some(user)) => Success(user)
        case Success(None)       => Failure(NotFoundException(s"User with id '$userId' was not found"))
      }
    }

    def getArenaUsersPaginated(page: Long, pageSize: Long, filterTeachers: Boolean, query: Option[String])(
        session: DBSession = ReadOnlyAutoSession
    ): Try[PaginatedArenaUsersDTO] = {
      val offset = (page - 1) * pageSize
      for {
        (totalCount, users) <- userRepository.getUsersPaginated(offset, pageSize, filterTeachers, query)(session)
        arenaUsers = users.map(ArenaUserDTO.from)
      } yield PaginatedArenaUsersDTO(totalCount, page, pageSize, arenaUsers)
    }

    def getMyNdlaUserDataDomain(
        feideAccessToken: Option[FeideAccessToken],
        arenaEnabledUsers: List[String]
    ): Try[MyNDLAUser] = {
      for {
        feideId <- feideApiClient.getFeideID(feideAccessToken)
        userData <- userRepository.rollbackOnFailure(session =>
          getOrCreateMyNDLAUserIfNotExist(feideId, feideAccessToken, arenaEnabledUsers)(session)
        )
      } yield userData
    }

    def getArenaEnabledUser(feideAccessToken: Option[FeideAccessToken]): Try[MyNDLAUser] = {
      for {
        users    <- configService.getMyNDLAEnabledUsers
        userData <- getMyNdlaUserDataDomain(feideAccessToken, users)
        orgs     <- configService.getMyNDLAEnabledOrgs
        arenaEnabled = folderConverterService.getArenaEnabled(userData, orgs)
        user <- if (arenaEnabled) Success(userData) else Failure(AccessDeniedException("User is not arena enabled"))
      } yield user
    }

    def getMyNDLAUserData(feideAccessToken: Option[FeideAccessToken]): Try[myndla.MyNDLAUserDTO] = {
      for {
        users    <- configService.getMyNDLAEnabledUsers
        userData <- getMyNdlaUserDataDomain(feideAccessToken, users)
        orgs     <- configService.getMyNDLAEnabledOrgs
        api = folderConverterService.toApiUserData(userData, orgs)
      } yield api
    }

    def getOrCreateMyNDLAUserIfNotExist(
        feideId: FeideID,
        feideAccessToken: Option[FeideAccessToken],
        arenaEnabledUsers: List[String]
    )(implicit session: DBSession): Try[MyNDLAUser] = {
      userRepository.reserveFeideIdIfNotExists(feideId)(session).flatMap {
        case false => createMyNDLAUser(feideId, feideAccessToken, arenaEnabledUsers)(session)
        case true =>
          userRepository.userWithFeideId(feideId)(session).flatMap {
            case None => Failure(new IllegalStateException(s"User with feide_id $feideId was not found."))
            case Some(userData) if userData.wasUpdatedLast24h => Success(userData)
            case Some(userData) =>
              fetchDataAndUpdateMyNDLAUser(feideId, feideAccessToken, userData, arenaEnabledUsers)(session)
          }
      }
    }

    def updateMyNDLAUserData(
        updatedUser: UpdatedMyNDLAUserDTO,
        feideAccessToken: Option[FeideAccessToken]
    ): Try[myndla.MyNDLAUserDTO] = {
      feideApiClient
        .getFeideID(feideAccessToken)
        .flatMap(feideId => updateFeideUserDataAuthenticated(updatedUser, feideId, feideAccessToken)(AutoSession))
    }

    def importUser(userData: myndla.MyNDLAUserDTO, feideId: FeideID, feideAccessToken: Option[FeideAccessToken])(
        implicit session: DBSession
    ): Try[myndla.MyNDLAUserDTO] =
      for {
        existingUser <- userService.getOrCreateMyNDLAUserIfNotExist(feideId, feideAccessToken, List.empty)(session)
        newFavorites = (existingUser.favoriteSubjects ++ userData.favoriteSubjects).distinct
        shareName    = existingUser.shareName || userData.shareName
        updatedFeideUser = UpdatedMyNDLAUserDTO(
          favoriteSubjects = Some(newFavorites),
          arenaEnabled = None,
          shareName = Some(shareName),
          arenaGroups = None,
          arenaAccepted = None
        )
        updated <- userService.updateFeideUserDataAuthenticated(updatedFeideUser, feideId, feideAccessToken)(session)
      } yield updated

    private def updateFeideUserDataAuthenticated(
        updatedUser: UpdatedMyNDLAUserDTO,
        feideId: FeideID,
        feideAccessToken: Option[FeideAccessToken]
    )(implicit session: DBSession): Try[myndla.MyNDLAUserDTO] = {
      for {
        _ <- folderWriteService.canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied(feideId, feideAccessToken)
        existingUserData <- getMyNDLAUserOrFail(feideId)
        enabledUsers     <- configService.getMyNDLAEnabledUsers
        combined = folderConverterService.mergeUserData(
          existingUserData,
          updatedUser,
          None,
          Some(existingUserData),
          enabledUsers
        )
        updated     <- userRepository.updateUser(feideId, combined)
        enabledOrgs <- configService.getMyNDLAEnabledOrgs
        api = folderConverterService.toApiUserData(updated, enabledOrgs)
      } yield api
    }

    def adminUpdateMyNDLAUserData(
        updatedUser: UpdatedMyNDLAUserDTO,
        feideId: Option[String],
        updaterToken: Option[TokenUser],
        updaterMyNdla: Option[MyNDLAUser]
    ): Try[myndla.MyNDLAUserDTO] = {
      feideId match {
        case None => Failure(ValidationException("feideId", "You need to supply a feideId to update a user."))
        case Some(id) =>
          for {
            existing     <- userService.getMyNDLAUserOrFail(id)
            enabledUsers <- configService.getMyNDLAEnabledUsers
            converted = folderConverterService.mergeUserData(
              existing,
              updatedUser,
              updaterToken,
              updaterMyNdla,
              enabledUsers
            )
            updated     <- userRepository.updateUser(id, converted)
            enabledOrgs <- configService.getMyNDLAEnabledOrgs
            api = folderConverterService.toApiUserData(updated, enabledOrgs)
          } yield api
      }
    }

    def adminUpdateMyNDLAUserData(
        userId: Long,
        updatedUser: UpdatedMyNDLAUserDTO,
        updaterMyNdla: MyNDLAUser
    )(session: DBSession = AutoSession): Try[myndla.MyNDLAUserDTO] = {
      for {
        existing     <- userService.getUserById(userId)(session)
        enabledUsers <- configService.getMyNDLAEnabledUsers
        converted = folderConverterService.mergeUserData(existing, updatedUser, None, Some(updaterMyNdla), enabledUsers)
        updated     <- userRepository.updateUserById(userId, converted)(session)
        enabledOrgs <- configService.getMyNDLAEnabledOrgs
        api = folderConverterService.toApiUserData(updated, enabledOrgs)
      } yield api
    }

    private[service] def getMyNDLAUserOrFail(feideId: FeideID): Try[MyNDLAUser] = {
      userRepository.userWithFeideId(feideId) match {
        case Failure(ex)         => Failure(ex)
        case Success(None)       => Failure(NotFoundException(s"User with feide_id $feideId was not found"))
        case Success(Some(user)) => Success(user)
      }
    }

    private def toDomainGroups(feideGroups: Seq[FeideGroup]): Seq[MyNDLAGroup] = {
      feideGroups
        .filter(group => group.`type` == FeideGroup.FC_ORG)
        .map(feideGroup =>
          MyNDLAGroup(
            id = feideGroup.id,
            displayName = feideGroup.displayName,
            isPrimarySchool = feideGroup.membership.primarySchool.getOrElse(false),
            parentId = feideGroup.parent
          )
        )
    }

    def getInitialIsArenaGroups(@unused feideId: FeideID): List[ArenaGroup] = {
      // NOTE: This exists to simplify mocking in tests until we have api user management
      List.empty
    }

    private def createMyNDLAUser(
        feideId: FeideID,
        feideAccessToken: Option[FeideAccessToken],
        arenaEnabledUsers: List[String]
    )(implicit
        session: DBSession
    ): Try[MyNDLAUser] = {
      for {
        feideExtendedUserData <- feideApiClient.getFeideExtendedUser(feideAccessToken)
        organization          <- feideApiClient.getOrganization(feideAccessToken)
        feideGroups           <- feideApiClient.getFeideGroups(feideAccessToken)
        newUser = MyNDLAUserDocument(
          favoriteSubjects = Seq.empty,
          userRole = if (feideExtendedUserData.isTeacher) UserRole.EMPLOYEE else UserRole.STUDENT,
          lastUpdated = clock.now().plusDays(1),
          organization = organization,
          groups = toDomainGroups(feideGroups),
          username = feideExtendedUserData.username,
          email = feideExtendedUserData.email,
          arenaEnabled = arenaEnabledUsers.map(_.toLowerCase).contains(feideExtendedUserData.email.toLowerCase),
          arenaGroups = getInitialIsArenaGroups(feideId),
          shareName = false,
          displayName = feideExtendedUserData.displayName,
          arenaAccepted = false
        )
        inserted <- userRepository.insertUser(feideId, newUser)(session)
      } yield inserted
    }

    private def fetchDataAndUpdateMyNDLAUser(
        feideId: FeideID,
        feideAccessToken: Option[FeideAccessToken],
        userData: MyNDLAUser,
        arenaEnabledUsers: List[String]
    )(implicit
        session: DBSession
    ): Try[MyNDLAUser] = {
      val feideUser    = feideApiClient.getFeideExtendedUser(feideAccessToken).?
      val organization = feideApiClient.getOrganization(feideAccessToken).?
      val feideGroups  = feideApiClient.getFeideGroups(feideAccessToken).?
      val updatedMyNDLAUser = MyNDLAUser(
        id = userData.id,
        feideId = userData.feideId,
        favoriteSubjects = userData.favoriteSubjects,
        userRole = if (feideUser.isTeacher) UserRole.EMPLOYEE else UserRole.STUDENT,
        lastUpdated = clock.now().plusDays(1),
        organization = organization,
        groups = toDomainGroups(feideGroups),
        username = feideUser.username,
        email = feideUser.email,
        arenaEnabled =
          userData.arenaEnabled || arenaEnabledUsers.map(_.toLowerCase).contains(feideUser.email.toLowerCase),
        shareName = userData.shareName,
        displayName = feideUser.displayName,
        arenaGroups = userData.arenaGroups,
        arenaAccepted = userData.arenaAccepted
      )
      userRepository.updateUser(feideId, updatedMyNDLAUser)(session)
    }

  }

}
