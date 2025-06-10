/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.controller

import no.ndla.common.errors.NotFoundException
import no.ndla.common.model.api.SingleResourceStatsDTO
import no.ndla.common.model.domain.ResourceType
import no.ndla.myndlaapi.model.api.StatsDTO
import no.ndla.myndlaapi.service.FolderReadService
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.TapirController
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import sttp.tapir.EndpointInput
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.model.CommaSeparated

import scala.util.Failure

trait StatsController {
  this: FolderReadService & TapirController =>
  class StatsController extends TapirController {
    override val serviceName: String                   = "stats"
    override protected val prefix: EndpointInput[Unit] = "myndla-api" / "v1" / serviceName

    def getStats: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Get stats")
      .description("Get stats")
      .out(jsonBody[StatsDTO])
      .errorOut(errorOutputsFor(404))
      .serverLogicPure { _ =>
        folderReadService.getStats
          .recoverNoneWith(Failure(NotFoundException("No stats found")))
      }

    private val pathResourceType =
      path[CommaSeparated[String]]("resourceType")
        .description(
          s"The type of the resource to look up. Comma separated list to support ${ResourceType.Multidisciplinary}. Possible values ${ResourceType.values
              .mkString(", ")}"
        )
    private val pathResourceIds =
      path[CommaSeparated[String]]("resourceIds").description("IDs of the resources to look up")

    private def getFolderResourceFavorites: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Get folder resource favorites")
      .description("Get folder resource favorites")
      .in("favorites" / pathResourceType / pathResourceIds)
      .out(jsonBody[List[SingleResourceStatsDTO]])
      .errorOut(errorOutputsFor(404))
      .serverLogicPure { case (resourceType, resourceIds) =>
        folderReadService.getFavouriteStatsForResource(resourceIds.values, resourceType.values)
      }

    private def getAllTheFavorites: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Get number of favorites for favorited resources")
      .description("Get number of favorites for favorited resources")
      .in("favorites")
      .out(jsonBody[Map[String, Map[String, Long]]])
      .errorOut(errorOutputsFor(400))
      .serverLogicPure { _ =>
        folderReadService.getAllTheFavorites
      }

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      getStats,
      getAllTheFavorites,
      getFolderResourceFavorites
    )
  }
}
