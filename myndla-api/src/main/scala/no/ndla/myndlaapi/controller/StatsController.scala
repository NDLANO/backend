/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.controller

import no.ndla.common.errors.NotFoundException
import no.ndla.common.model.api.SingleResourceStats
import no.ndla.myndlaapi.Eff
import no.ndla.myndlaapi.model.api.Stats
import no.ndla.myndlaapi.service.FolderReadService
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.{Service, TapirErrorHelpers}
import no.ndla.network.tapir.TapirErrors.errorOutputsFor
import sttp.tapir.EndpointInput
import sttp.tapir.server.ServerEndpoint
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.model.CommaSeparated

trait StatsController {
  this: FolderReadService with TapirErrorHelpers =>
  class StatsController extends Service[Eff] {
    override val serviceName: String                   = "stats"
    override protected val prefix: EndpointInput[Unit] = "myndla-api" / "v1" / serviceName

    def getStats: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Get stats")
      .description("Get stats")
      .out(jsonBody[Stats])
      .errorOut(errorOutputsFor(404))
      .serverLogicPure { _ =>
        folderReadService.getStats match {
          case Some(c) => Right(c)
          case None    => returnLeftError(NotFoundException("No stats found"))
        }
      }
    private val pathResourceType =
      path[CommaSeparated[String]]("resourceType").description("The type of the resource to look up")
    private val pathResourceIds =
      path[CommaSeparated[String]]("resourceIds").description("IDs of the resources to look up")

    def getFolderResourceFavorites: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Get folder resource favorites")
      .description("Get folder resource favorites")
      .in("favorites" / pathResourceType / pathResourceIds)
      .out(jsonBody[List[SingleResourceStats]])
      .errorOut(errorOutputsFor(404))
      .serverLogicPure { case (resourceType, resourceIds) =>
        folderReadService.getFavouriteStatsForResource(resourceIds.values, resourceType.values).handleErrorsOrOk
      }

    def getAllTheFavorites: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Get number of favorites for favorited resources")
      .description("Get number of favorites for favorited resources")
      .in("favorites")
      .out(jsonBody[Map[String, Map[String, Long]]])
      .errorOut(errorOutputsFor(400))
      .serverLogicPure { _ =>
        folderReadService.getAllTheFavorites.handleErrorsOrOk
      }

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      getStats,
      getAllTheFavorites,
      getFolderResourceFavorites
    )
  }
}
