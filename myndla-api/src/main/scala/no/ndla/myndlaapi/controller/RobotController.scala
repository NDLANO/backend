/*
 * Part of NDLA myndla-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.controller

import no.ndla.myndlaapi.model.api.robot.{CreateRobotDefinitionDTO, ListOfRobotDefinitionsDTO, RobotDefinitionDTO}
import no.ndla.myndlaapi.model.domain.RobotStatus
import no.ndla.myndlaapi.service.RobotService
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.Parameters.feideHeader
import no.ndla.network.tapir.TapirController
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint

import java.util.UUID

trait RobotController {
  this: ErrorHandling & TapirController & RobotService =>
  lazy val robotController: RobotController

  class RobotController extends TapirController {
    override val serviceName: String         = "robots"
    override val prefix: EndpointInput[Unit] = "myndla-api" / "v1" / serviceName

    private def createNewRobotDefinition: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Create a new robot definition")
      .description("Create a new robot definition")
      .in(feideHeader)
      .in(jsonBody[CreateRobotDefinitionDTO])
      .errorOut(errorOutputsFor(400, 401, 403))
      .out(jsonBody[RobotDefinitionDTO])
      .serverLogicPure { case (feide, robotDefinitionDTO) =>
        robotService.createRobot(robotDefinitionDTO, feide)
      }

    private def getAllRobotDefinitions: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("List out all of your own robot definitions")
      .description("List out all of your own robot definitions")
      .in(feideHeader)
      .errorOut(errorOutputsFor(400, 401, 403))
      .out(jsonBody[ListOfRobotDefinitionsDTO])
      .serverLogicPure { feide => robotService.getAllRobots(feide) }

    private def getSingleRobotDefinition: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Get single robot definition")
      .description("Get single robot definition")
      .in(feideHeader)
      .in(path[UUID]("robot-id"))
      .errorOut(errorOutputsFor(400, 401, 403, 404))
      .out(jsonBody[RobotDefinitionDTO])
      .serverLogicPure { case (feide, robotId) => robotService.getSingleRobot(robotId, feide) }

    private def updateRobotDefinition(): ServerEndpoint[Any, Eff] = endpoint.put
      .summary("Update a robot definition")
      .description("Update a robot definition")
      .in(feideHeader)
      .in(path[UUID]("robot-id"))
      .errorOut(errorOutputsFor(400, 401, 403))
      .in(jsonBody[CreateRobotDefinitionDTO])
      .out(jsonBody[RobotDefinitionDTO])
      .serverLogicPure { case (token, robotId, robotDefinitionDTO) =>
        robotService.updateRobot(robotId, robotDefinitionDTO, token)
      }

    private def updateRobotStatus(): ServerEndpoint[Any, Eff] = endpoint.put
      .summary("Update a robot definition status")
      .description("Update a robot definition status")
      .in(feideHeader)
      .in(path[UUID]("robot-id") / path[RobotStatus]("robot-status"))
      .errorOut(errorOutputsFor(400, 401, 403))
      .out(jsonBody[RobotDefinitionDTO])
      .serverLogicPure { case (token, robotId, newStatus) =>
        robotService.updateRobotStatus(robotId, newStatus, token)
      }

    private def deleteRobotDefinition(): ServerEndpoint[Any, Eff] = endpoint.delete
      .summary("Delete a robot definition")
      .description("Delete a robot definition")
      .in(feideHeader)
      .in(path[UUID]("robot-id"))
      .errorOut(errorOutputsFor(400, 401, 403))
      .out(noContent)
      .serverLogicPure { case (feideToken, robotId) =>
        robotService.deleteRobot(robotId, feideToken)
      }

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      createNewRobotDefinition,
      getAllRobotDefinitions,
      getSingleRobotDefinition,
      updateRobotDefinition(),
      updateRobotStatus(),
      deleteRobotDefinition()
    )
  }
}
