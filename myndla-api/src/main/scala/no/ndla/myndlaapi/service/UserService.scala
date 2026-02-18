/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.service

import cats.implicits.*
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.Clock
import no.ndla.common.aws.NdlaEmailClient
import no.ndla.common.errors.{AccessDeniedException, InactivityEmailException, NotFoundException}
import no.ndla.common.implicits.*
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.myndla
import no.ndla.common.model.api.myndla.UpdatedMyNDLAUserDTO
import no.ndla.common.model.domain.myndla.{MyNDLAGroup, MyNDLAUser, MyNDLAUserDocument, UserRole}
import no.ndla.database.{DBUtility, ReadableDbSession}
import no.ndla.myndlaapi.Props
import no.ndla.myndlaapi.integration.nodebb.NodeBBClient
import no.ndla.myndlaapi.model.api.InactiveUserResultDTO
import no.ndla.myndlaapi.repository.{FolderRepository, UserRepository}
import no.ndla.network.clients.{FeideApiClient, FeideGroup}
import no.ndla.network.model.{FeideAccessToken, FeideID, FeideUserWrapper}
import scalikejdbc.DBSession

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
    emailClient: NdlaEmailClient,
    props: Props,
) extends StrictLogging {
  def getMyNDLAUser(feideId: FeideID, feideAccessToken: Option[FeideAccessToken])(implicit
      session: DBSession
  ): Try[MyNDLAUser] = {
    for {
      user     <- getOrCreateMyNDLAUserIfNotExist(feideId, feideAccessToken)(using session)
      lastSeen <- userRepository.updateLastSeen(feideId, NDLADate.now())(using session)
    } yield user.copy(lastSeen = lastSeen)
  }

  def getMyNdlaUserDataDomain(feideAccessToken: Option[FeideAccessToken]): Try[MyNDLAUser] = {
    for {
      feideId  <- feideApiClient.getFeideID(feideAccessToken)
      userData <- dbUtility.rollbackOnFailure(session => getMyNDLAUser(feideId, feideAccessToken)(using session))
    } yield userData
  }

  def getMyNdlaUserDataDTO(feideAccessToken: Option[FeideAccessToken]): Try[myndla.MyNDLAUserDTO] = {
    for {
      userData <- getMyNdlaUserDataDomain(feideAccessToken)
      apiData   = folderConverterService.toApiUserData(userData)
    } yield apiData
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

    for {
      alreadyExists <- userRepository.reserveFeideIdIfNotExists(feideId)(using session)
      r             <- alreadyExists match {
        case false => createMyNDLAUser(feideId, feideAccessToken)(using session)
        case true  => for {
            data   <- userRepository.userWithFeideId(feideId)(using session)
            result <- data match {
              case None                                         => Failure(new IllegalStateException(s"User with feide_id $feideId was not found."))
              case Some(userData) if userData.wasUpdatedLast24h => Success(userData)
              case Some(userData)                               => fetchDataAndUpdateMyNDLAUser(feideId, feideAccessToken, userData)(using session)
            }
          } yield result
      }
    } yield r
  }

  def updateMyNDLAUserData(updatedUser: UpdatedMyNDLAUserDTO, feide: FeideUserWrapper): Try[myndla.MyNDLAUserDTO] =
    dbUtility.writeSession { implicit session =>
      updateFeideUserDataAuthenticated(updatedUser, feide)
    }

  def importUser(userData: myndla.MyNDLAUserDTO, feide: FeideUserWrapper)(implicit
      session: DBSession
  ): Try[myndla.MyNDLAUserDTO] = for {
    existingUser    <- feide.userOrAccessDenied
    newFavorites     = existingUser.favoriteSubjects ++ userData.favoriteSubjects
    updatedFeideUser = UpdatedMyNDLAUserDTO(favoriteSubjects = Some(newFavorites.distinct), arenaEnabled = None)
    updated         <- updateFeideUserDataAuthenticated(updatedFeideUser, feide)(using session)
  } yield updated

  private def updateFeideUserDataAuthenticated(updatedUser: UpdatedMyNDLAUserDTO, feide: FeideUserWrapper)(implicit
      session: DBSession
  ): Try[myndla.MyNDLAUserDTO] = {
    for {
      user     <- feide.userOrAccessDenied
      _        <- folderWriteService.canWriteOrAccessDenied(feide)
      combined <- folderConverterService.mergeUserData(user, updatedUser, None)
      updated  <- userRepository.updateUser(user.feideId, combined)
      api       = folderConverterService.toApiUserData(updated)
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

    val lastSeen = NDLADate.now()

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
      lastSeen = lastSeen,
    )
    userRepository.updateUser(feideId, updatedMyNDLAUser)(using session)
  }

  def deleteAllUserData(feide: FeideUserWrapper): Try[Unit] = dbUtility.rollbackOnFailure(session => {
    for {
      user         <- feide.userOrAccessDenied
      nodebbUserId <- nodeBBClient.getUserId(feide.token)
      _            <- folderRepository.deleteAllUserFolders(user.feideId)(using session)
      _            <- folderRepository.deleteAllUserResources(user.feideId)(using session)
      _            <- nodeBBClient.deleteUser(nodebbUserId, feide.token)
      _            <- userRepository.deleteUser(user.feideId)(using session)
    } yield ()
  })

  private val emailAfter           = 180
  private val deleteAfter          = 210
  private def emailSubject: String = "Min NDLA brukeren din blir snart slettet"
  private def emailBody: String    = s"""Hei!<br>
                                        |<br>
                                        |Du har ikke brukt kontoen din på Min NDLA på en stund.<br>
                                        |Kontoer som ikke har vært i bruk på $deleteAfter dager, blir slettet. Kontoen din har nå vært inaktiv i $emailAfter dager.<br>
                                        |<br>
                                        |Ønsker du å beholde kontoen? Da må du logge inn på Min NDLA i løpet av de neste ${deleteAfter - emailAfter} dagene.<br>
                                        |Hvis du ikke lenger har behov for kontoen, trenger du ikke å gjøre noe.<br>
                                        |<br>
                                        |Har du spørsmål, kan du lese mer på <a href="https://ndla.no">ndla.no</a> eller sende oss en e-post på <a href="mailto:hjelp@ndla.no">hjelp@ndla.no</a>.<br>
                                        |<br>
                                        |Vennlig hilsen<br>
                                        |NDLA<br>
                                        |""".stripMargin

  def sendInactivityEmail(user: MyNDLAUser): Try[Boolean] = {
    props.Environment match {
      case "prod" =>
        logger.info(s"Sending inactivity email to user ${user.feideId} at email ${user.email}")
        emailClient.sendEmail(user.email, emailSubject, emailBody)
      case _ =>
        logger.info(s"Skipping sending inactivity email to user ${user.feideId} in non-prod environment")
        Success(true)
    }
  }

  def sendInactivityEmailIgnoreEnvironment(email: String): Try[Boolean] =
    emailClient.sendEmail(email, emailSubject, emailBody)

  private def getUsersToEmail(now: NDLADate)(implicit session: ReadableDbSession): Try[List[MyNDLAUser]] = for {
    lastCleanupRun      <- userRepository.getLastCleanup
    emailCandidates     <- userRepository.getUserNotSeenSince(now.minusDays(emailAfter))
    usersToEmailFiltered = lastCleanupRun match {
      case None          => emailCandidates
      case Some(lastRun) =>
        // NOTE: This is the cutoff for which users would have been sent an email in the last run.
        //       Since we only want to email users once, we filter out users that would have been emailed in the last run, even if they are still inactive.
        val lastEmailCutoff = lastRun.lastCleanupDate.minusDays(emailAfter)
        emailCandidates.filter(user => user.lastSeen.isAfter(lastEmailCutoff))
    }
  } yield usersToEmailFiltered

  private def sendInactivityEmails(usersToEmail: List[MyNDLAUser]) = usersToEmail.traverse(user =>
    sendInactivityEmail(user).flatMap {
      case true  => Success(())
      case false => Failure(InactivityEmailException(s"Failed to send inactivity email to user ${user.id}"))
    }
  )

  def cleanupInactiveUsers(): Try[InactiveUserResultDTO] = dbUtility.writeSession { implicit session =>
    val now              = clock.now()
    val deleteBeforeDate = now.minusDays(deleteAfter)

    for {
      usersToDelete <- userRepository.getUserNotSeenSince(deleteBeforeDate)
      deleteFeideIds = usersToDelete.map(_.feideId).toSet
      usersToEmail  <- getUsersToEmail(now).map(_.filterNot(user => deleteFeideIds.contains(user.feideId)))
      _             <- sendInactivityEmails(usersToEmail)
      _             <- usersToDelete.traverse(user => userRepository.deleteUser(user.feideId).map(_ => ()))
      cleanupResult <- userRepository.insertCleanupResult(usersToDelete.size, usersToEmail.size, now)
    } yield InactiveUserResultDTO(cleanupResult.numCleanup, cleanupResult.numEmailed)
  }
}
