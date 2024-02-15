/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.controller

import no.ndla.common.errors.NotFoundException
import no.ndla.myndla.model.api.{SingleResourceStats, Stats}
import no.ndla.myndla.service.FolderReadService
import no.ndla.myndlaapi.Eff
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.{Service, TapirErrorHelpers}
import no.ndla.network.tapir.TapirErrors.errorOutputsFor
import sttp.tapir.EndpointInput
import sttp.tapir.server.ServerEndpoint
import sttp.tapir._
import sttp.tapir.generic.auto._

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
    private val pathResourceType = path[String]("resourceType").description("The type of the resource to look up")
    private val pathResourceId   = path[String]("resourceId").description("ID of the resource to look up")

    def getFolderResourceFavorites: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Get folder resource favorites")
      .description("Get folder resource favorites")
      .in("favorites" / pathResourceType / pathResourceId)
      .out(jsonBody[SingleResourceStats])
      .errorOut(errorOutputsFor(404))
      .serverLogicPure { case (resourceType, resourceId) =>
        folderReadService.getFavouriteStatsForResource(resourceId, resourceType).handleErrorsOrOk
      }

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      getStats,
      getFolderResourceFavorites
    )
  }
}
