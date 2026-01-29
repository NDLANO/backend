/*
 * Part of NDLA myndla-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.controller

import no.ndla.myndlaapi.integration.InternalMyNDLAApiClient
import no.ndla.myndlaapi.model.api.robot.{CreateRobotDefinitionDTO, ListOfRobotDefinitionsDTO, RobotDefinitionDTO}
import no.ndla.myndlaapi.model.domain.RobotStatus
import no.ndla.myndlaapi.service.RobotService
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.{ErrorHelpers, TapirController}
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint

import java.util.UUID

class RobotController(using
    robotService: RobotService,
    errorHandling: ControllerErrorHandling,
    errorHelpers: ErrorHelpers,
    myNDLAApiClient: InternalMyNDLAApiClient,
) extends TapirController {
  override val serviceName: String         = "robots"
  override val prefix: EndpointInput[Unit] = "myndla-api" / "v1" / serviceName

  private def createNewRobotDefinition: ServerEndpoint[Any, Eff] = endpoint
    .post
    .summary("Create a new robot definition")
    .description("Create a new robot definition")
    .in(jsonBody[CreateRobotDefinitionDTO])
    .errorOut(errorOutputsFor(400, 401, 403))
    .out(jsonBody[RobotDefinitionDTO])
    .withFeideUser
    .serverLogicPure { feide =>
      { robotDefinitionDTO =>
        robotService.createRobot(robotDefinitionDTO, feide)
      }
    }

  private def getAllRobotDefinitions: ServerEndpoint[Any, Eff] = endpoint
    .get
    .summary("List out all of your own robot definitions")
    .description("List out all of your own robot definitions")
    .errorOut(errorOutputsFor(400, 401, 403))
    .out(jsonBody[ListOfRobotDefinitionsDTO])
    .withFeideUser
    .serverLogicPure { feide => _ =>
      robotService.getAllRobots(feide)
    }

  private def getSingleRobotDefinition: ServerEndpoint[Any, Eff] = endpoint
    .get
    .summary("Get single robot definition")
    .description("Get single robot definition")
    .in(path[UUID]("robot-id"))
    .errorOut(errorOutputsFor(400, 401, 403, 404))
    .out(jsonBody[RobotDefinitionDTO])
    .withFeideUser
    .serverLogicPure { feide =>
      { robotId =>
        robotService.getSingleRobot(robotId, feide)
      }
    }

  private def updateRobotDefinition(): ServerEndpoint[Any, Eff] = endpoint
    .put
    .summary("Update a robot definition")
    .description("Update a robot definition")
    .in(path[UUID]("robot-id"))
    .errorOut(errorOutputsFor(400, 401, 403))
    .in(jsonBody[CreateRobotDefinitionDTO])
    .out(jsonBody[RobotDefinitionDTO])
    .withFeideUser
    .serverLogicPure {
      case feide => { case (robotId, robotDefinitionDTO) =>
        robotService.updateRobot(robotId, robotDefinitionDTO, feide)
      }
    }

  private def updateRobotStatus(): ServerEndpoint[Any, Eff] = endpoint
    .put
    .summary("Update a robot definition status")
    .description("Update a robot definition status")
    .in(path[UUID]("robot-id") / path[RobotStatus]("robot-status"))
    .errorOut(errorOutputsFor(400, 401, 403))
    .out(jsonBody[RobotDefinitionDTO])
    .withFeideUser
    .serverLogicPure { feide =>
      { case (robotId, newStatus) =>
        robotService.updateRobotStatus(robotId, newStatus, feide)
      }
    }

  private def deleteRobotDefinition(): ServerEndpoint[Any, Eff] = endpoint
    .delete
    .summary("Delete a robot definition")
    .description("Delete a robot definition")
    .in(path[UUID]("robot-id"))
    .errorOut(errorOutputsFor(400, 401, 403))
    .out(noContent)
    .withFeideUser
    .serverLogicPure { feide => robotId =>
      robotService.deleteRobot(robotId, feide)
    }

  override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
    createNewRobotDefinition,
    getAllRobotDefinitions,
    getSingleRobotDefinition,
    updateRobotDefinition(),
    updateRobotStatus(),
    deleteRobotDefinition(),
  )
}
