/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.controller

import io.circe.generic.auto._
import no.ndla.myndla.MyNDLAAuthHelpers
import no.ndla.myndla.service.UserService
import no.ndla.myndlaapi.Eff
import no.ndla.myndlaapi.model.arena.api.{Category, CategoryWithTopics, NewPost, NewTopic, Topic}
import no.ndla.myndlaapi.service.ArenaReadService
import no.ndla.network.clients.FeideApiClient
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.TapirErrors.errorOutputsFor
import no.ndla.network.tapir.{Service, TapirErrorHelpers}
import sttp.tapir.generic.auto._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{EndpointInput, _}

trait ArenaController {
  this: ErrorHelpers
    with TapirErrorHelpers
    with MyNDLAAuthHelpers
    with ArenaReadService
    with FeideApiClient
    with UserService =>
  val arenaController: ArenaController

  class ArenaController extends Service[Eff] {
    import MyNDLAAuthHelpers.authlessEndpointFeideExtension
    override protected val prefix: EndpointInput[Unit] = "myndla-api" / "v1" / "arena"

    val pathCategoryId = path[Long]("categoryId").description("The category id")
    val pathTopicId    = path[Long]("topicId").description("The topic id")

    def getCategories: ServerEndpoint[Any, Eff] = endpoint.get
      .in("categories")
      .summary("Get all arena categories")
      .description("Get db configuration by key")
      .out(jsonBody[List[Category]])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { _ => _ =>
        arenaReadService.getCategories.handleErrorsOrOk
      }

    def getCategory: ServerEndpoint[Any, Eff] = endpoint.get
      .in("categories" / pathCategoryId)
      .summary("Get single arena category")
      .description("Get single arena category")
      .out(jsonBody[CategoryWithTopics])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { _ => categoryId =>
        arenaReadService.getCategory(categoryId).handleErrorsOrOk
      }

    def getTopic: ServerEndpoint[Any, Eff] = endpoint.get
      .in("topics" / pathTopicId)
      .summary("Get single arena topic")
      .description("Get single arena topic")
      .out(jsonBody[Topic])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { _ => topicId =>
        arenaReadService.getTopic(topicId).handleErrorsOrOk
      }

    def postTopic: ServerEndpoint[Any, Eff] = endpoint.post
      .in("categories" / pathCategoryId / "topics")
      .summary("Post arena topic")
      .description("Post arena topic")
      .in(jsonBody[NewTopic])
      .out(jsonBody[Topic])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user =>
        { case (categoryId, newTopic) =>
          arenaReadService.postTopic(categoryId, newTopic, user).handleErrorsOrOk
        }
      }

    def postPostToTopic: ServerEndpoint[Any, Eff] = endpoint.post
      .in("topics" / pathTopicId / "posts")
      .summary("Post arena post to topic")
      .description("Post arena post to topic")
      .in(jsonBody[NewPost])
      .out(jsonBody[Topic])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user =>
        { case (topicId, newPost) =>
          arenaReadService.postPost(topicId, newPost, user).handleErrorsOrOk
        }
      }

    override protected val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      getCategories,
      getCategory,
      getTopic,
      postTopic,
      postPostToTopic
    )
  }

}
