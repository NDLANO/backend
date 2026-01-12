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
import no.ndla.common.implicits.*
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

class UserService(using
    feideApiClient: FeideApiClient,
    folderConverterService: FolderConverterService,
    userRepository: UserRepository,
    clock: Clock,
    folderWriteService: => FolderWriteService,
    nodeBBClient: NodeBBClient,
    folderRepository: FolderRepository,
    dbUtility: DBUtility,
) {
  def getMyNDLAUser(feideId: FeideID, feideAccessToken: Option[FeideAccessToken]): Try[MyNDLAUser] = {
    dbUtility.rollbackOnFailure(session => getOrCreateMyNDLAUserIfNotExist(feideId, feideAccessToken)(using session))
  }

  def getMyNdlaUserDataDomain(feideAccessToken: Option[FeideAccessToken]): Try[MyNDLAUser] = {
    for {
      feideId  <- feideApiClient.getFeideID(feideAccessToken)
      userData <- dbUtility.rollbackOnFailure(session =>
        getOrCreateMyNDLAUserIfNotExist(feideId, feideAccessToken)(using session)
      )
    } yield userData
  }

  def getArenaEnabledUser(feideAccessToken: Option[FeideAccessToken]): Try[MyNDLAUser] = {
    for {
      userData <- getMyNdlaUserDataDomain(feideAccessToken)
      user     <-
        if (userData.arenaEnabled) Success(userData)
        else Failure(AccessDeniedException("User is not arena enabled"))
    } yield user
  }

  def getMyNDLAUserData(feideAccessToken: Option[FeideAccessToken]): Try[myndla.MyNDLAUserDTO] = {
    for {
      userData <- getMyNdlaUserDataDomain(feideAccessToken)
      api       = folderConverterService.toApiUserData(userData)
    } yield api
  }

  private def getOrCreateMyNDLAUserIfNotExist(feideId: FeideID, feideAccessToken: Option[FeideAccessToken])(implicit
      session: DBSession
  ): Try[MyNDLAUser] = {
    userRepository
      .reserveFeideIdIfNotExists(feideId)(using session)
      .flatMap {
        case false => createMyNDLAUser(feideId, feideAccessToken)(using session)
        case true  => userRepository
            .userWithFeideId(feideId)(using session)
            .flatMap {
              case None                                         => Failure(new IllegalStateException(s"User with feide_id $feideId was not found."))
              case Some(userData) if userData.wasUpdatedLast24h => Success(userData)
              case Some(userData)                               => fetchDataAndUpdateMyNDLAUser(feideId, feideAccessToken, userData)(using session)
            }
      }
  }

  def updateMyNDLAUserData(
      updatedUser: UpdatedMyNDLAUserDTO,
      feideAccessToken: Option[FeideAccessToken],
  ): Try[myndla.MyNDLAUserDTO] = {
    feideApiClient
      .getFeideID(feideAccessToken)
      .flatMap(feideId => updateFeideUserDataAuthenticated(updatedUser, feideId, feideAccessToken)(using AutoSession))
  }

  def importUser(userData: myndla.MyNDLAUserDTO, feideId: FeideID, feideAccessToken: Option[FeideAccessToken])(implicit
      session: DBSession
  ): Try[myndla.MyNDLAUserDTO] = for {
    existingUser <- getOrCreateMyNDLAUserIfNotExist(feideId, feideAccessToken)(using session)
    newFavorites  = (
      existingUser.favoriteSubjects ++ userData.favoriteSubjects
    ).distinct
    updatedFeideUser = UpdatedMyNDLAUserDTO(favoriteSubjects = Some(newFavorites), arenaEnabled = None)
    updated         <- updateFeideUserDataAuthenticated(updatedFeideUser, feideId, feideAccessToken)(using session)
  } yield updated

  private def updateFeideUserDataAuthenticated(
      updatedUser: UpdatedMyNDLAUserDTO,
      feideId: FeideID,
      feideAccessToken: Option[FeideAccessToken],
  )(implicit session: DBSession): Try[myndla.MyNDLAUserDTO] = {
    for {
      _                <- folderWriteService.canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied(feideId, feideAccessToken)
      existingUserData <- getMyNDLAUserOrFail(feideId)
      combined         <- folderConverterService.mergeUserData(existingUserData, updatedUser, None)
      updated          <- userRepository.updateUser(feideId, combined)
      api               = folderConverterService.toApiUserData(updated)
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
          parentId = feideGroup.parent,
        )
      )
  }

  private def createMyNDLAUser(feideId: FeideID, feideAccessToken: Option[FeideAccessToken])(implicit
      session: DBSession
  ): Try[MyNDLAUser] = {
    for {
      feideExtendedUserData <- feideApiClient.getFeideExtendedUser(feideAccessToken)
      organization          <- feideApiClient.getOrganization(feideAccessToken)
      feideGroups           <- feideApiClient.getFeideGroups(feideAccessToken)
      userRole               =
        if (feideExtendedUserData.isTeacher) UserRole.EMPLOYEE
        else UserRole.STUDENT
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
      )
      inserted <- userRepository.insertUser(feideId, newUser)(using session)
    } yield inserted
  }

  private def fetchDataAndUpdateMyNDLAUser(
      feideId: FeideID,
      feideAccessToken: Option[FeideAccessToken],
      userData: MyNDLAUser,
  )(implicit session: DBSession): Try[MyNDLAUser] = permitTry {
    val feideUser    = feideApiClient.getFeideExtendedUser(feideAccessToken).?
    val organization = feideApiClient.getOrganization(feideAccessToken).?
    val feideGroups  = feideApiClient.getFeideGroups(feideAccessToken).?
    val userRole     =
      if (feideUser.isTeacher) UserRole.EMPLOYEE
      else UserRole.STUDENT
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
    )
    userRepository.updateUser(feideId, updatedMyNDLAUser)(using session)
  }

  def deleteAllUserData(feideAccessToken: Option[FeideAccessToken]): Try[Unit] =
    dbUtility.rollbackOnFailure(session => {
      for {
        feideToken   <- feideApiClient.getFeideAccessTokenOrFail(feideAccessToken)
        feideId      <- feideApiClient.getFeideID(feideAccessToken)
        nodebbUserId <- nodeBBClient.getUserId(feideToken)
        _            <- folderRepository.deleteAllUserFolders(feideId)(using session)
        _            <- folderRepository.deleteAllUserResources(feideId)(using session)
        _            <- nodeBBClient.deleteUser(nodebbUserId, feideToken)
        _            <- userRepository.deleteUser(feideId)(using session)
      } yield ()
    })
}
