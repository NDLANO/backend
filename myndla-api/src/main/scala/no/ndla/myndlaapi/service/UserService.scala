/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.service

import no.ndla.common.Clock
import no.ndla.common.errors.{AccessDeniedException, NotFoundException}
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
        feideAccessToken: Option[FeideAccessToken]
    ): Try[MyNDLAUser] = {
      for {
        feideId <- feideApiClient.getFeideID(feideAccessToken)
        userData <- userRepository.rollbackOnFailure(session =>
          getOrCreateMyNDLAUserIfNotExist(feideId, feideAccessToken)(session)
        )
      } yield userData
    }

    def getArenaEnabledUser(feideAccessToken: Option[FeideAccessToken]): Try[MyNDLAUser] = {
      for {
        userData <- getMyNdlaUserDataDomain(feideAccessToken)
        user <-
          if (userData.arenaEnabled) Success(userData) else Failure(AccessDeniedException("User is not arena enabled"))
      } yield user
    }

    def getMyNDLAUserData(feideAccessToken: Option[FeideAccessToken]): Try[myndla.MyNDLAUserDTO] = {
      for {
        userData <- getMyNdlaUserDataDomain(feideAccessToken)
        api = folderConverterService.toApiUserData(userData)
      } yield api
    }

    def getOrCreateMyNDLAUserIfNotExist(
        feideId: FeideID,
        feideAccessToken: Option[FeideAccessToken]
    )(implicit session: DBSession): Try[MyNDLAUser] = {
      userRepository.reserveFeideIdIfNotExists(feideId)(session).flatMap {
        case false => createMyNDLAUser(feideId, feideAccessToken)(session)
        case true =>
          userRepository.userWithFeideId(feideId)(session).flatMap {
            case None => Failure(new IllegalStateException(s"User with feide_id $feideId was not found."))
            case Some(userData) if userData.wasUpdatedLast24h => Success(userData)
            case Some(userData) =>
              fetchDataAndUpdateMyNDLAUser(feideId, feideAccessToken, userData)(session)
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
        existingUser <- userService.getOrCreateMyNDLAUserIfNotExist(feideId, feideAccessToken)(session)
        newFavorites = (existingUser.favoriteSubjects ++ userData.favoriteSubjects).distinct
        updatedFeideUser = UpdatedMyNDLAUserDTO(
          favoriteSubjects = Some(newFavorites),
          arenaEnabled = None,
          arenaGroups = None,
          arenaAccepted = None,
          shareNameAccepted = None
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
        combined <- folderConverterService.mergeUserData(
          existingUserData,
          updatedUser,
          None,
          Some(existingUserData),
          feideAccessToken
        )
        updated <- userRepository.updateUser(feideId, combined)
        api = folderConverterService.toApiUserData(updated)
      } yield api
    }

    def adminUpdateMyNDLAUserData(
        userId: Long,
        updatedUser: UpdatedMyNDLAUserDTO,
        updaterToken: Option[TokenUser],
        updaterMyNdla: Option[MyNDLAUser]
    )(session: DBSession = AutoSession): Try[myndla.MyNDLAUserDTO] = {
      for {
        existing <- userService.getUserById(userId)(session)
        converted <- folderConverterService.mergeUserData(
          existing,
          updatedUser,
          updaterToken,
          updaterMyNdla,
          // NOTE: This token is used to create a nodebb profile
          //       since the one updating here is an admin, we cannot use it to create a profile.
          feideToken = None
        )
        updated <- userRepository.updateUserById(userId, converted)(session)
        api = folderConverterService.toApiUserData(updated)
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
        feideAccessToken: Option[FeideAccessToken]
    )(implicit
        session: DBSession
    ): Try[MyNDLAUser] = {
      for {
        feideExtendedUserData <- feideApiClient.getFeideExtendedUser(feideAccessToken)
        organization          <- feideApiClient.getOrganization(feideAccessToken)
        feideGroups           <- feideApiClient.getFeideGroups(feideAccessToken)
        userRole = if (feideExtendedUserData.isTeacher) UserRole.EMPLOYEE else UserRole.STUDENT
        newUser = MyNDLAUserDocument(
          favoriteSubjects = Seq.empty,
          userRole = userRole,
          lastUpdated = clock.now().plusDays(1),
          organization = organization,
          groups = toDomainGroups(feideGroups),
          username = feideExtendedUserData.username,
          displayName = feideExtendedUserData.displayName,
          email = feideExtendedUserData.email,
          arenaEnabled = userRole == UserRole.EMPLOYEE,
          arenaAccepted = false,
          arenaGroups = getInitialIsArenaGroups(feideId),
          shareNameAccepted = false
        )
        inserted <- userRepository.insertUser(feideId, newUser)(session)
      } yield inserted
    }

    private def fetchDataAndUpdateMyNDLAUser(
        feideId: FeideID,
        feideAccessToken: Option[FeideAccessToken],
        userData: MyNDLAUser
    )(implicit
        session: DBSession
    ): Try[MyNDLAUser] = {
      val feideUser    = feideApiClient.getFeideExtendedUser(feideAccessToken).?
      val organization = feideApiClient.getOrganization(feideAccessToken).?
      val feideGroups  = feideApiClient.getFeideGroups(feideAccessToken).?
      val userRole     = if (feideUser.isTeacher) UserRole.EMPLOYEE else UserRole.STUDENT
      val updatedMyNDLAUser = MyNDLAUser(
        id = userData.id,
        feideId = userData.feideId,
        favoriteSubjects = userData.favoriteSubjects,
        userRole = userRole,
        lastUpdated = clock.now().plusDays(1),
        organization = organization,
        groups = toDomainGroups(feideGroups),
        username = feideUser.username,
        displayName = feideUser.displayName,
        email = feideUser.email,
        arenaEnabled = userData.arenaEnabled || userRole == UserRole.EMPLOYEE,
        arenaAccepted = userData.arenaAccepted,
        arenaGroups = userData.arenaGroups,
        shareNameAccepted = userData.shareNameAccepted
      )
      userRepository.updateUser(feideId, updatedMyNDLAUser)(session)
    }

  }

}
