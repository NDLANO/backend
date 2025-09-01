/*
 * Part of NDLA myndla-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.service

import no.ndla.common.Clock
import no.ndla.common.errors.NotFoundException
import no.ndla.common.implicits.*
import no.ndla.database.DBUtility
import no.ndla.myndlaapi.model.api.robot.{CreateRobotDefinitionDTO, ListOfRobotDefinitionsDTO, RobotDefinitionDTO}
import no.ndla.myndlaapi.model.domain.{RobotConfiguration, RobotDefinition, RobotStatus}
import no.ndla.myndlaapi.repository.RobotRepository
import no.ndla.network.clients.FeideApiClient
import no.ndla.network.model.FeideAccessToken

import java.util.UUID
import scala.util.Try

class RobotService(using
    robotRepository: RobotRepository,
    dbUtility: DBUtility,
    clock: Clock,
    feideApiClient: FeideApiClient,
    folderWriteService: FolderWriteService
) {

  def getAllRobots(feideToken: Option[FeideAccessToken]): Try[ListOfRobotDefinitionsDTO] = dbUtility.readOnly {
    session =>
      for {
        feideId <- feideApiClient.getFeideID(feideToken)
        robots  <- robotRepository.getRobotsWithFeideId(feideId)(using session)
      } yield ListOfRobotDefinitionsDTO(robots = robots.map(RobotDefinitionDTO.fromDomain))
  }

  def getSingleRobot(robotId: UUID, feide: Option[FeideAccessToken]): Try[RobotDefinitionDTO] = dbUtility.readOnly {
    session =>
      lazy val nfe = NotFoundException(s"Could not find robot definition with id $robotId")
      for {
        feideId    <- feideApiClient.getFeideID(feide)
        maybeRobot <- robotRepository.getRobotWithId(robotId)(using session)
        robot      <- maybeRobot.toTry(nfe)
        _          <- robot.canRead(feideId, notFound = true)
      } yield RobotDefinitionDTO.fromDomain(robot)
  }

  def createRobot(
      robotDefinitionDTO: CreateRobotDefinitionDTO,
      feideToken: Option[FeideAccessToken]
  ): Try[RobotDefinitionDTO] =
    dbUtility.rollbackOnFailure { session =>
      val now = clock.now()
      for {
        feideId <- feideApiClient.getFeideID(feideToken)
        _       <- folderWriteService.canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied(feideId, feideToken)
        domain = RobotDefinition(
          id = UUID.randomUUID(),
          feideId = feideId,
          status = robotDefinitionDTO.status,
          configuration = RobotConfiguration.fromDTO(robotDefinitionDTO.configuration),
          created = now,
          updated = now,
          shared = Option.when(robotDefinitionDTO.status == RobotStatus.SHARED)(now)
        )
        robot <- robotRepository.insertRobotDefinition(domain)(session)
      } yield RobotDefinitionDTO.fromDomain(robot)
    }

  def updateRobot(
      robotId: UUID,
      robotDefinitionDTO: CreateRobotDefinitionDTO,
      feideToken: Option[FeideAccessToken]
  ): Try[RobotDefinitionDTO] = updateRobotWith(robotId, feideToken) {
    _.copy(
      status = robotDefinitionDTO.status,
      configuration = RobotConfiguration.fromDTO(robotDefinitionDTO.configuration)
    )
  }

  def updateRobotStatus(
      robotId: UUID,
      newStatus: RobotStatus,
      feideToken: Option[FeideAccessToken]
  ): Try[RobotDefinitionDTO] =
    updateRobotWith(robotId, feideToken) {
      _.copy(status = newStatus)
    }

  def deleteRobot(robotId: UUID, feideToken: Option[FeideAccessToken]): Try[Unit] = dbUtility.rollbackOnFailure {
    session =>
      for {
        feideId       <- feideApiClient.getFeideID(feideToken)
        _             <- folderWriteService.canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied(feideId, feideToken)
        maybeRobot    <- robotRepository.getRobotWithId(robotId)(using session)
        existingRobot <- maybeRobot.toTry(NotFoundException(s"Could not find editable robot with id '$robotId'"))
        _             <- existingRobot.canEdit(feideId)
        _             <- robotRepository.deleteRobotDefinition(robotId)(using session)
      } yield ()
  }

  private def updateRobotWith(robotId: UUID, feideToken: Option[FeideAccessToken])(
      updateFunc: RobotDefinition => RobotDefinition
  ): Try[RobotDefinitionDTO] =
    dbUtility.rollbackOnFailure { session =>
      val now = clock.now()
      for {
        feideId       <- feideApiClient.getFeideID(feideToken)
        _             <- folderWriteService.canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied(feideId, feideToken)
        maybeRobot    <- robotRepository.getRobotWithId(robotId)(using session)
        existingRobot <- maybeRobot.toTry(NotFoundException(s"Could not find editable robot with id '$robotId'"))
        _             <- existingRobot.canEdit(feideId)
        updated       <- Try(updateFunc(existingRobot))
        sharedTime = updated.status match {
          case RobotStatus.SHARED if existingRobot.shared.isEmpty => Some(now)
          case RobotStatus.SHARED                                 => existingRobot.shared
          case _                                                  => None
        }
        withUpdatedTimes = updated.copy(updated = now, shared = sharedTime)
        _ <- robotRepository.updateRobotDefinition(withUpdatedTimes)(using session)

      } yield RobotDefinitionDTO.fromDomain(withUpdatedTimes)
    }

}
