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
import no.ndla.myndlaapi.model.arena.api.{
  Category,
  CategoryWithTopics,
  NewCategory,
  NewPost,
  NewTopic,
  Paginated,
  Post,
  Topic
}
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
    override val serviceName: String                   = "Arena"
    override protected val prefix: EndpointInput[Unit] = "myndla-api" / "v1" / "arena"

    private val pathCategoryId = path[Long]("categoryId").description("The category id")
    private val pathTopicId    = path[Long]("topicId").description("The topic id")
    private val pathPostId     = path[Long]("postId").description("The post id")

    private val queryPage     = query[Long]("page").default(1).validate(Validator.min(1))
    private val queryPageSize = query[Long]("page-size").default(10).validate(Validator.inRange(1, 100))

    def getCategories: ServerEndpoint[Any, Eff] = endpoint.get
      .in("categories")
      .summary("Get all categories")
      .description("Get all categories")
      .out(jsonBody[List[Category]])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { _ => _ =>
        arenaReadService.getCategories().handleErrorsOrOk
      }

    def getCategory: ServerEndpoint[Any, Eff] = endpoint.get
      .in("categories" / pathCategoryId)
      .summary("Get single category")
      .description("Get single category")
      .out(jsonBody[CategoryWithTopics])
      .in(queryPage)
      .in(queryPageSize)
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { _ =>
        { case (categoryId, page, pageSize) =>
          arenaReadService.getCategory(categoryId, page, pageSize)().handleErrorsOrOk
        }
      }

    def getCategoryTopics: ServerEndpoint[Any, Eff] = endpoint.get
      .in("categories" / pathCategoryId / "topics")
      .summary("Get single category")
      .description("Get single category")
      .out(jsonBody[Paginated[Topic]])
      .in(queryPage)
      .in(queryPageSize)
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { _ =>
        { case (categoryId, page, pageSize) =>
          arenaReadService.getTopicsForCategory(categoryId, page, pageSize)().handleErrorsOrOk
        }
      }

    def getTopic: ServerEndpoint[Any, Eff] = endpoint.get
      .in("topics" / pathTopicId)
      .summary("Get single topic")
      .description("Get single topic")
      .out(jsonBody[Topic])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { _ => topicId =>
        arenaReadService.getTopic(topicId).handleErrorsOrOk
      }

    def getRecentTopics: ServerEndpoint[Any, Eff] = endpoint.get
      .in("topics" / "recent")
      .summary("Get recent topics")
      .description("Get recent topics")
      .in(queryPage)
      .in(queryPageSize)
      .out(jsonBody[Paginated[Topic]])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { _ =>
        { case (page, pageSize) =>
          arenaReadService.getRecentTopics(page, pageSize)().handleErrorsOrOk
        }
      }

    def postTopic: ServerEndpoint[Any, Eff] = endpoint.post
      .in("categories" / pathCategoryId / "topics")
      .summary("Create new topic")
      .description("Create new topic")
      .in(jsonBody[NewTopic])
      .out(jsonBody[Topic])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user =>
        { case (categoryId, newTopic) =>
          arenaReadService.postTopic(categoryId, newTopic, user).handleErrorsOrOk
        }
      }

    def editTopic: ServerEndpoint[Any, Eff] = endpoint.put
      .in("topics" / pathTopicId)
      .summary("Edit a topic")
      .description("Edit a topic")
      .in(jsonBody[NewTopic])
      .out(jsonBody[Topic])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user =>
        { case (topicId, newTopic) =>
          arenaReadService.updateTopic(topicId, newTopic, user)().handleErrorsOrOk
        }
      }

    def postPostToTopic: ServerEndpoint[Any, Eff] = endpoint.post
      .in("topics" / pathTopicId / "posts")
      .summary("Add post to topic")
      .description("Add post to topic")
      .in(jsonBody[NewPost])
      .out(jsonBody[Topic])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user =>
        { case (topicId, newPost) =>
          arenaReadService.postPost(topicId, newPost, user).handleErrorsOrOk
        }
      }

    def postCategory: ServerEndpoint[Any, Eff] = endpoint.post
      .in("categories")
      .summary("Create new arena category")
      .description("Create new arena category")
      .in(jsonBody[NewCategory])
      .out(jsonBody[Category])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArenaAdmin = true)
      .serverLogicPure { _ =>
        { case newCategory =>
          arenaReadService.newCategory(newCategory)().handleErrorsOrOk
        }
      }

    def updateCategory: ServerEndpoint[Any, Eff] = endpoint.put
      .in("categories" / pathCategoryId)
      .summary("Update arena category")
      .description("Update arena category")
      .in(jsonBody[NewCategory])
      .out(jsonBody[Category])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArenaAdmin = true)
      .serverLogicPure { _ =>
        { case (categoryId, newCategory) =>
          arenaReadService.updateCategory(categoryId, newCategory)().handleErrorsOrOk
        }
      }

    def editPost: ServerEndpoint[Any, Eff] = endpoint.put
      .in("posts" / pathPostId)
      .summary("Update arena post")
      .description("Update arena post")
      .in(jsonBody[NewPost])
      .out(jsonBody[Post])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user =>
        { case (postId, newPost) =>
          arenaReadService.updatePost(postId, newPost, user)().handleErrorsOrOk
        }
      }

    def deletePost: ServerEndpoint[Any, Eff] = endpoint.delete
      .in("posts" / pathPostId)
      .summary("Delete arena post")
      .description("Delete arena post")
      .out(emptyOutput)
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user => postId =>
        arenaReadService.deletePost(postId, user)().handleErrorsOrOk
      }

    override protected val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      getCategories,
      getCategory,
      getCategoryTopics,
      getRecentTopics,
      getTopic,
      postTopic,
      editTopic,
      postPostToTopic,
      editPost,
      deletePost,
      postCategory,
      updateCategory
    )
  }

}
