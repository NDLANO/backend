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
import no.ndla.network.model.FeideUserWrapper

import java.util.UUID
import scala.util.Try

class RobotService(using
    robotRepository: RobotRepository,
    dbUtility: DBUtility,
    clock: Clock,
    folderWriteService: FolderWriteService,
) {

  def getAllRobots(feide: FeideUserWrapper): Try[ListOfRobotDefinitionsDTO] = dbUtility.writeSession { session =>
    for {
      user   <- feide.userOrAccessDenied
      robots <- robotRepository.getRobotsWithFeideId(user.feideId)(using session)
    } yield ListOfRobotDefinitionsDTO(robots = robots.map(RobotDefinitionDTO.fromDomain))
  }

  def getSingleRobot(robotId: UUID, feide: FeideUserWrapper): Try[RobotDefinitionDTO] =
    dbUtility.writeSession { session =>
      lazy val nfe = NotFoundException(s"Could not find robot definition with id $robotId")
      for {
        user       <- feide.userOrAccessDenied
        maybeRobot <- robotRepository.getRobotWithId(robotId)(using session)
        robot      <- maybeRobot.toTry(nfe)
        _          <- robot.canRead(user.feideId, notFound = true)
      } yield RobotDefinitionDTO.fromDomain(robot)
    }

  def createRobot(robotDefinitionDTO: CreateRobotDefinitionDTO, feide: FeideUserWrapper): Try[RobotDefinitionDTO] =
    dbUtility.rollbackOnFailure { session =>
      val now = clock.now()
      for {
        user  <- folderWriteService.canWriteOrAccessDenied(feide)
        domain = RobotDefinition(
          id = UUID.randomUUID(),
          feideId = user.feideId,
          status = robotDefinitionDTO.status,
          configuration = RobotConfiguration.fromDTO(robotDefinitionDTO.configuration),
          created = now,
          updated = now,
          shared = Option.when(robotDefinitionDTO.status == RobotStatus.SHARED)(now),
        )
        robot <- robotRepository.insertRobotDefinition(domain)(session)
      } yield RobotDefinitionDTO.fromDomain(robot)
    }

  def updateRobot(
      robotId: UUID,
      robotDefinitionDTO: CreateRobotDefinitionDTO,
      feide: FeideUserWrapper,
  ): Try[RobotDefinitionDTO] = updateRobotWith(robotId, feide) {
    _.copy(
      status = robotDefinitionDTO.status,
      configuration = RobotConfiguration.fromDTO(robotDefinitionDTO.configuration),
    )
  }

  def updateRobotStatus(robotId: UUID, newStatus: RobotStatus, feideToken: FeideUserWrapper): Try[RobotDefinitionDTO] =
    updateRobotWith(robotId, feideToken) {
      _.copy(status = newStatus)
    }

  def deleteRobot(robotId: UUID, feide: FeideUserWrapper): Try[Unit] = dbUtility.rollbackOnFailure { session =>
    for {
      user          <- folderWriteService.canWriteOrAccessDenied(feide)
      maybeRobot    <- robotRepository.getRobotWithId(robotId)(using session)
      existingRobot <- maybeRobot.toTry(NotFoundException(s"Could not find editable robot with id '$robotId'"))
      _             <- existingRobot.canEdit(user.feideId)
      _             <- robotRepository.deleteRobotDefinition(robotId)(using session)
    } yield ()
  }

  private def updateRobotWith(robotId: UUID, feide: FeideUserWrapper)(
      updateFunc: RobotDefinition => RobotDefinition
  ): Try[RobotDefinitionDTO] = dbUtility.rollbackOnFailure { session =>
    val now = clock.now()
    for {
      user          <- folderWriteService.canWriteOrAccessDenied(feide)
      maybeRobot    <- robotRepository.getRobotWithId(robotId)(using session)
      existingRobot <- maybeRobot.toTry(NotFoundException(s"Could not find editable robot with id '$robotId'"))
      _             <- existingRobot.canEdit(user.feideId)
      updated       <- Try(updateFunc(existingRobot))
      sharedTime     = updated.status match {
        case RobotStatus.SHARED if existingRobot.shared.isEmpty => Some(now)
        case RobotStatus.SHARED                                 => existingRobot.shared
        case _                                                  => None
      }
      withUpdatedTimes = updated.copy(updated = now, shared = sharedTime)
      _               <- robotRepository.updateRobotDefinition(withUpdatedTimes)(using session)

    } yield RobotDefinitionDTO.fromDomain(withUpdatedTimes)
  }

}
