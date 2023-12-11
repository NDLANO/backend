/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.controller

import io.circe.generic.auto._
import no.ndla.myndlaapi.Eff
import no.ndla.myndlaapi.model.arena.api.{Category, CategoryWithTopics}
import no.ndla.myndlaapi.service.ArenaReadService
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.Parameters.feideHeader
import no.ndla.network.tapir.TapirErrors.errorOutputsFor
import no.ndla.network.tapir.{Service, TapirErrorHelpers}
import sttp.tapir.{EndpointInput, _}
import sttp.tapir.codec.enumeratum._
import sttp.tapir.generic.auto._
import sttp.tapir.server.ServerEndpoint

trait ArenaController {
  this: ErrorHelpers with TapirErrorHelpers with ArenaReadService =>

  val arenaController: ArenaController

  class ArenaController extends Service[Eff] {
    override protected val prefix: EndpointInput[Unit] = "myndla-api" / "v1" / "arena"

    import ErrorHelpers._

    def getCategories: ServerEndpoint[Any, Eff] = endpoint.get
      .in("categories")
      .summary("Get all arena categories")
      .description("Get db configuration by key")
      .in(feideHeader)
      .out(jsonBody[List[Category]])
      .errorOut(errorOutputsFor(401, 403, 404))
      .serverLogicPure { _ =>
        arenaReadService.getCategories.handleErrorsOrOk
      }

    def getCategory: ServerEndpoint[Any, Eff] = endpoint.get
      .in("categories" / path[Long]("categoryId"))
      .summary("Get single arena category")
      .description("Get single arena category")
      .in(feideHeader)
      .out(jsonBody[CategoryWithTopics])
      .errorOut(errorOutputsFor(401, 403, 404))
      .serverLogicPure { case (categoryId, feideHeader) =>
        arenaReadService.getCategory(categoryId).handleErrorsOrOk
      }

    override protected val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      getCategories,
      getCategory
    )
  }

}
