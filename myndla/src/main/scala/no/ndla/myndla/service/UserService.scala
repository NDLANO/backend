/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.service

import no.ndla.common.Clock
import no.ndla.common.errors.{AccessDeniedException, NotFoundException, ValidationException}
import no.ndla.common.implicits.TryQuestionMark
import no.ndla.myndla.model.{api, domain}
import no.ndla.myndla.repository.UserRepository
import no.ndla.network.clients.{FeideApiClient, FeideGroup}
import no.ndla.network.model.{FeideAccessToken, FeideID}
import no.ndla.network.tapir.auth.TokenUser
import scalikejdbc.{AutoSession, DBSession}

import scala.annotation.unused
import scala.util.{Failure, Success, Try}

trait UserService {
  this: FeideApiClient
    with FolderConverterService
    with ConfigService
    with UserRepository
    with Clock
    with FolderWriteService =>

  val userService: UserService

  class UserService {

    def getMyNdlaUserDataDomain(feideAccessToken: Option[FeideAccessToken]): Try[domain.MyNDLAUser] = {
      for {
        feideId  <- feideApiClient.getFeideID(feideAccessToken)
        userData <- getOrCreateMyNDLAUserIfNotExist(feideId, feideAccessToken)(AutoSession)
      } yield userData
    }

    def getArenaEnabledUser(feideAccessToken: Option[FeideAccessToken]): Try[domain.MyNDLAUser] = {
      for {
        userData <- getMyNdlaUserDataDomain(feideAccessToken)
        orgs     <- configService.getMyNDLAEnabledOrgs
        arenaEnabled = folderConverterService.getArenaEnabled(userData, orgs)
        user <- if (arenaEnabled) Success(userData) else Failure(AccessDeniedException("User is not arena enabled"))
      } yield user
    }

    def getMyNDLAUserData(feideAccessToken: Option[FeideAccessToken]): Try[api.MyNDLAUser] = {
      for {
        userData <- getMyNdlaUserDataDomain(feideAccessToken)
        orgs     <- configService.getMyNDLAEnabledOrgs
        api = folderConverterService.toApiUserData(userData, orgs)
      } yield api
    }

    def getOrCreateMyNDLAUserIfNotExist(
        feideId: FeideID,
        feideAccessToken: Option[FeideAccessToken]
    )(implicit session: DBSession): Try[domain.MyNDLAUser] = {
      userRepository.userWithFeideId(feideId)(session).flatMap {
        case None                                         => createMyNDLAUser(feideId, feideAccessToken)(session)
        case Some(userData) if userData.wasUpdatedLast24h => Success(userData)
        case Some(userData) => fetchDataAndUpdateMyNDLAUser(feideId, feideAccessToken, userData)(session)
      }
    }

    def updateMyNDLAUserData(
        updatedUser: api.UpdatedMyNDLAUser,
        feideAccessToken: Option[FeideAccessToken]
    ): Try[api.MyNDLAUser] = {
      feideApiClient
        .getFeideID(feideAccessToken)
        .flatMap(feideId => updateFeideUserDataAuthenticated(updatedUser, feideId, feideAccessToken)(AutoSession))
    }

    def importUser(userData: api.MyNDLAUser, feideId: FeideID, feideAccessToken: Option[FeideAccessToken])(implicit
        session: DBSession
    ): Try[api.MyNDLAUser] =
      for {
        existingUser <- userService.getOrCreateMyNDLAUserIfNotExist(feideId, feideAccessToken)(session)
        newFavorites = (existingUser.favoriteSubjects ++ userData.favoriteSubjects).distinct
        shareName    = existingUser.shareName || userData.shareName
        updatedFeideUser = api.UpdatedMyNDLAUser(
          favoriteSubjects = Some(newFavorites),
          arenaEnabled = None,
          shareName = Some(shareName)
        )
        updated <- userService.updateFeideUserDataAuthenticated(updatedFeideUser, feideId, feideAccessToken)(session)
      } yield updated

    private def updateFeideUserDataAuthenticated(
        updatedUser: api.UpdatedMyNDLAUser,
        feideId: FeideID,
        feideAccessToken: Option[FeideAccessToken]
    )(implicit session: DBSession): Try[api.MyNDLAUser] = {
      for {
        _ <- folderWriteService.canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied(feideId, feideAccessToken)
        existingUserData <- getMyNDLAUserOrFail(feideId)
        combined = folderConverterService.mergeUserData(existingUserData, updatedUser, None)
        updated     <- userRepository.updateUser(feideId, combined)
        enabledOrgs <- configService.getMyNDLAEnabledOrgs
        api = folderConverterService.toApiUserData(updated, enabledOrgs)
      } yield api
    }

    def adminUpdateMyNDLAUserData(
        updatedUser: api.UpdatedMyNDLAUser,
        feideId: Option[String],
        user: TokenUser
    ): Try[api.MyNDLAUser] = {
      feideId match {
        case None => Failure(ValidationException("feideId", "You need to supply a feideId to update a user."))
        case Some(id) =>
          for {
            existing <- userService.getMyNDLAUserOrFail(id)
            converted = folderConverterService.mergeUserData(existing, updatedUser, Some(user))
            updated     <- userRepository.updateUser(id, converted)
            enabledOrgs <- configService.getMyNDLAEnabledOrgs
            api = folderConverterService.toApiUserData(updated, enabledOrgs)
          } yield api
      }
    }

    private[service] def getMyNDLAUserOrFail(feideId: FeideID): Try[domain.MyNDLAUser] = {
      userRepository.userWithFeideId(feideId) match {
        case Failure(ex)         => Failure(ex)
        case Success(None)       => Failure(NotFoundException(s"User with feide_id $feideId was not found"))
        case Success(Some(user)) => Success(user)
      }
    }

    private def toDomainGroups(feideGroups: Seq[FeideGroup]): Seq[domain.MyNDLAGroup] = {
      feideGroups
        .filter(group => group.`type` == FeideGroup.FC_ORG)
        .map(feideGroup =>
          domain.MyNDLAGroup(
            id = feideGroup.id,
            displayName = feideGroup.displayName,
            isPrimarySchool = feideGroup.membership.primarySchool.getOrElse(false),
            parentId = feideGroup.parent
          )
        )
    }

    def getInitialIsArenaAdmin(@unused feideId: FeideID): Option[Boolean] = {
      // NOTE: This exists to simplify mocking in tests until we have api user management
      Some(false)
    }

    private def createMyNDLAUser(feideId: FeideID, feideAccessToken: Option[FeideAccessToken])(implicit
        session: DBSession
    ): Try[domain.MyNDLAUser] = {
      for {
        feideExtendedUserData <- feideApiClient.getFeideExtendedUser(feideAccessToken)
        organization          <- feideApiClient.getOrganization(feideAccessToken)
        feideGroups           <- feideApiClient.getFeideGroups(feideAccessToken)
        newUser = domain.MyNDLAUserDocument(
          favoriteSubjects = Seq.empty,
          userRole = if (feideExtendedUserData.isTeacher) domain.UserRole.EMPLOYEE else domain.UserRole.STUDENT,
          lastUpdated = clock.now().plusDays(1),
          organization = organization,
          groups = toDomainGroups(feideGroups),
          username = feideExtendedUserData.username,
          email = feideExtendedUserData.email,
          arenaEnabled = false,
          arenaAdmin = getInitialIsArenaAdmin(feideId),
          shareName = false,
          displayName = feideExtendedUserData.displayName
        )
        inserted <- userRepository.insertUser(feideId, newUser)(session)
      } yield inserted
    }

    private def fetchDataAndUpdateMyNDLAUser(
        feideId: FeideID,
        feideAccessToken: Option[FeideAccessToken],
        userData: domain.MyNDLAUser
    )(implicit
        session: DBSession
    ): Try[domain.MyNDLAUser] = {
      val feideUser    = feideApiClient.getFeideExtendedUser(feideAccessToken).?
      val organization = feideApiClient.getOrganization(feideAccessToken).?
      val feideGroups  = feideApiClient.getFeideGroups(feideAccessToken).?
      val updatedMyNDLAUser = domain.MyNDLAUser(
        id = userData.id,
        feideId = userData.feideId,
        favoriteSubjects = userData.favoriteSubjects,
        userRole = if (feideUser.isTeacher) domain.UserRole.EMPLOYEE else domain.UserRole.STUDENT,
        lastUpdated = clock.now().plusDays(1),
        organization = organization,
        groups = toDomainGroups(feideGroups),
        username = feideUser.username,
        email = feideUser.email,
        arenaEnabled = userData.arenaEnabled,
        shareName = userData.shareName,
        displayName = feideUser.displayName,
        arenaAdmin = userData.arenaAdmin
      )
      userRepository.updateUser(feideId, updatedMyNDLAUser)(session)
    }

  }

}
