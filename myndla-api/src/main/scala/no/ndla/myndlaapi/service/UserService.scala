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
import no.ndla.common.model.domain.myndla.{MyNDLAGroup, MyNDLAUser, MyNDLAUserDocument, UserRole}
import no.ndla.database.DBUtility
import no.ndla.myndlaapi.integration.nodebb.NodeBBClient
import no.ndla.myndlaapi.repository.{FolderRepository, UserRepository}
import no.ndla.network.clients.{FeideApiClient, FeideGroup}
import no.ndla.network.model.{FeideAccessToken, FeideID}
import scalikejdbc.{AutoSession, DBSession}

import scala.util.{Failure, Success, Try}

trait UserService {
  this: FeideApiClient & FolderConverterService & ConfigService & UserRepository & Clock & FolderWriteService &
    NodeBBClient & FolderRepository & DBUtility =>

  val userService: UserService

  class UserService {
    def getMyNdlaUserDataDomain(
        feideAccessToken: Option[FeideAccessToken]
    ): Try[MyNDLAUser] = {
      for {
        feideId <- feideApiClient.getFeideID(feideAccessToken)
        userData <- DBUtil.rollbackOnFailure(session =>
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
          feideAccessToken
        )
        updated <- userRepository.updateUser(feideId, combined)
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
          shareNameAccepted = true
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
        shareNameAccepted = userData.shareNameAccepted
      )
      userRepository.updateUser(feideId, updatedMyNDLAUser)(session)
    }

    def deleteAllUserData(feideAccessToken: Option[FeideAccessToken]): Try[Unit] =
      DBUtil.rollbackOnFailure(session => {
        for {
          feideToken   <- feideApiClient.getFeideAccessTokenOrFail(feideAccessToken)
          feideId      <- feideApiClient.getFeideID(feideAccessToken)
          nodebbUserId <- nodebb.getUserId(feideToken)
          _            <- folderRepository.deleteAllUserFolders(feideId)(session)
          _            <- folderRepository.deleteAllUserResources(feideId)(session)
          _            <- userRepository.deleteUser(feideId)(session)
          _            <- nodebb.deleteUser(nodebbUserId, feideToken)
        } yield ()
      })
  }
}
