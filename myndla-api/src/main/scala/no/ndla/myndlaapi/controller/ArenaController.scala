/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.controller

import io.circe.generic.auto._
import no.ndla.myndla.MyNDLAAuthHelpers
import no.ndla.myndla.model.api.{ArenaUser, PaginatedArenaUsers}
import no.ndla.myndla.service.UserService
import no.ndla.myndlaapi.Eff
import no.ndla.myndlaapi.model.arena.api.{
  Category,
  CategorySort,
  CategoryWithTopics,
  Flag,
  NewCategory,
  NewFlag,
  NewPost,
  NewTopic,
  PaginatedNewPostNotifications,
  PaginatedPosts,
  PaginatedTopics,
  Post,
  Topic,
  TopicWithPosts
}
import no.ndla.myndlaapi.service.ArenaReadService
import no.ndla.network.clients.FeideApiClient
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.TapirErrors.errorOutputsFor
import no.ndla.network.tapir.{Service, TapirErrorHelpers}
import sttp.model.StatusCode
import sttp.tapir.generic.auto._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir._

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
    override val serviceName: String                   = "arena"
    override protected val prefix: EndpointInput[Unit] = "myndla-api" / "v1" / serviceName

    private val pathCategoryId     = path[Long]("categoryId").description("The category id")
    private val pathTopicId        = path[Long]("topicId").description("The topic id")
    private val pathPostId         = path[Long]("postId").description("The post id")
    private val pathFlagId         = path[Long]("flagId").description("The flag id")
    private val pathNotificationId = path[Long]("notificationId").description("The notification id")

    private val queryPage     = query[Long]("page").default(1).validate(Validator.min(1))
    private val queryPageSize = query[Long]("page-size").default(10).validate(Validator.inRange(1, 100))

    def getCategories: ServerEndpoint[Any, Eff] = endpoint.get
      .in("categories")
      .summary("Get all categories")
      .description("Get all categories")
      .in(query[Boolean]("followed").description("Filter on followed categories").default(false))
      .in(query[CategorySort]("sort").description("Sort categories").default(CategorySort.ByRank))
      .out(jsonBody[List[Category]])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user =>
        { case (followed, sort) =>
          arenaReadService.getCategories(user, followed, sort)().handleErrorsOrOk
        }
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
      .serverLogicPure { user =>
        { case (categoryId, page, pageSize) =>
          arenaReadService.getCategory(categoryId, page, pageSize, user)().handleErrorsOrOk
        }
      }

    def getCategoryTopics: ServerEndpoint[Any, Eff] = endpoint.get
      .in("categories" / pathCategoryId / "topics")
      .summary("Get topics for a category")
      .description("Get topics for a category")
      .out(jsonBody[PaginatedTopics])
      .in(queryPage)
      .in(queryPageSize)
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user =>
        { case (categoryId, page, pageSize) =>
          arenaReadService.getTopicsForCategory(categoryId, page, pageSize, user)().handleErrorsOrOk
        }
      }

    def sortCategories: ServerEndpoint[Any, Eff] = endpoint.put
      .in("categories" / "sort")
      .summary("Sort categories")
      .description("Sort categories")
      .in(jsonBody[List[Long]].description("List of category ids in the order they should be sorted"))
      .out(jsonBody[List[Category]])
      .errorOut(errorOutputsFor(401, 403))
      .requireMyNDLAUser(requireArenaAdmin = true)
      .serverLogicPure {
        user =>
          { sortedIds =>
            arenaReadService.sortCategories(sortedIds, user).handleErrorsOrOk
          }
      }

    def getTopic: ServerEndpoint[Any, Eff] = endpoint.get
      .in("topics" / pathTopicId)
      .summary("Get single topic")
      .description("Get single topic")
      .out(jsonBody[TopicWithPosts])
      .errorOut(errorOutputsFor(401, 403, 404, 410))
      .in(queryPage)
      .in(queryPageSize)
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user =>
        { case (topicId, page, pageSize) =>
          arenaReadService.getTopic(topicId, user, page, pageSize)().handleErrorsOrOk
        }
      }

    def getRecentTopics: ServerEndpoint[Any, Eff] = endpoint.get
      .in("topics" / "recent")
      .summary("Get recent topics")
      .description("Get recent topics")
      .in(queryPage)
      .in(queryPageSize)
      .in(query[Option[Long]]("user-id").description("A users id to filter on"))
      .out(jsonBody[PaginatedTopics])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user =>
        { case (page, pageSize, ownerId) =>
          arenaReadService.getRecentTopics(page, pageSize, ownerId, user)().handleErrorsOrOk
        }
      }

    def followCategory: ServerEndpoint[Any, Eff] = endpoint.post
      .in("categories" / pathCategoryId / "follow")
      .summary("Follow category")
      .description("Follow category")
      .out(jsonBody[CategoryWithTopics])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user => categoryId =>
        arenaReadService.followCategory(categoryId, user)().handleErrorsOrOk
      }

    def unfollowCategory: ServerEndpoint[Any, Eff] = endpoint.post
      .in("categories" / pathCategoryId / "unfollow")
      .summary("Unfollow category")
      .description("Unfollow category")
      .out(jsonBody[CategoryWithTopics])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user => categoryId =>
        arenaReadService.unfollowCategory(categoryId, user)().handleErrorsOrOk
      }

    def followTopic: ServerEndpoint[Any, Eff] = endpoint.post
      .in("topics" / pathTopicId / "follow")
      .summary("Follow topic")
      .description("Follow topic")
      .out(jsonBody[TopicWithPosts])
      .errorOut(errorOutputsFor(401, 403, 404, 410))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user => topicId =>
        arenaReadService.followTopic(topicId, user)().handleErrorsOrOk
      }

    def unfollowTopic: ServerEndpoint[Any, Eff] = endpoint.post
      .in("topics" / pathTopicId / "unfollow")
      .summary("Unfollow topic")
      .description("Unfollow topic")
      .out(jsonBody[TopicWithPosts])
      .errorOut(errorOutputsFor(401, 403, 404, 410))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user => topicId =>
        arenaReadService.unfollowTopic(topicId, user)().handleErrorsOrOk
      }

    def postTopic: ServerEndpoint[Any, Eff] = endpoint.post
      .in("categories" / pathCategoryId / "topics")
      .summary("Create new topic")
      .description("Create new topic")
      .in(jsonBody[NewTopic])
      .out(statusCode(StatusCode.Created).and(jsonBody[Topic]))
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
      .errorOut(errorOutputsFor(401, 403, 404, 410))
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
      .out(statusCode(StatusCode.Created).and(jsonBody[Topic]))
      .errorOut(errorOutputsFor(401, 403, 404, 410))
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
      .out(statusCode(StatusCode.Created).and(jsonBody[Category]))
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArenaAdmin = true)
      .serverLogicPure { _ => newCategory =>
        arenaReadService.newCategory(newCategory)().handleErrorsOrOk
      }

    def updateCategory: ServerEndpoint[Any, Eff] = endpoint.put
      .in("categories" / pathCategoryId)
      .summary("Update arena category")
      .description("Update arena category")
      .in(jsonBody[NewCategory])
      .out(jsonBody[Category])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArenaAdmin = true)
      .serverLogicPure { user =>
        { case (categoryId, newCategory) =>
          arenaReadService.updateCategory(categoryId, newCategory, user)().handleErrorsOrOk
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

    def deleteTopic: ServerEndpoint[Any, Eff] = endpoint.delete
      .in("topics" / pathTopicId)
      .summary("Delete arena topic")
      .description("Delete arena topic")
      .out(emptyOutput)
      .errorOut(errorOutputsFor(401, 403, 404, 410))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user => topicId =>
        arenaReadService.deleteTopic(topicId, user)().handleErrorsOrOk
      }

    def deleteCategory: ServerEndpoint[Any, Eff] = endpoint.delete
      .in("categories" / pathCategoryId)
      .summary("Delete arena category")
      .description("Delete arena category")
      .out(emptyOutput)
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArenaAdmin = true)
      .serverLogicPure { user => categoryId =>
        arenaReadService.deleteCategory(categoryId, user)().handleErrorsOrOk
      }

    def flagPost: ServerEndpoint[Any, Eff] = endpoint.post
      .in("posts" / pathPostId / "flag")
      .summary("Flag arena post")
      .description("Flag arena post")
      .out(emptyOutput)
      .in(jsonBody[NewFlag])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user =>
        { case (postId, newFlag) =>
          arenaReadService.flagPost(postId, user, newFlag)().handleErrorsOrOk
        }
      }

    def resolveFlag: ServerEndpoint[Any, Eff] = endpoint.put
      .in("flags" / pathFlagId)
      .summary("Toggle arena flag resolution status")
      .description("Toggle arena flag resolution status")
      .out(jsonBody[Flag])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArenaAdmin = true)
      .serverLogicPure { _ => flagId =>
        arenaReadService.resolveFlag(flagId)().handleErrorsOrOk
      }

    def getFlags: ServerEndpoint[Any, Eff] = endpoint.get
      .in("flags")
      .summary("List flagged posts")
      .description("List flagged posts")
      .in(queryPage)
      .in(queryPageSize)
      .out(jsonBody[PaginatedPosts])
      .errorOut(errorOutputsFor(401, 403))
      .requireMyNDLAUser(requireArenaAdmin = true)
      .serverLogicPure { user =>
        { case (page, pageSize) =>
          arenaReadService.getFlaggedPosts(page, pageSize, user)().handleErrorsOrOk
        }
      }

    def getNotifications: ServerEndpoint[Any, Eff] = endpoint.get
      .in("notifications")
      .summary("Get your notifications")
      .description("Get your notifications")
      .in(queryPage)
      .in(queryPageSize)
      .out(jsonBody[PaginatedNewPostNotifications])
      .errorOut(errorOutputsFor(401, 403))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user =>
        { case (page, pageSize) =>
          arenaReadService.getNotifications(user, page, pageSize)().handleErrorsOrOk
        }
      }

    def markSingleNotificationAsRead: ServerEndpoint[Any, Eff] = endpoint.post
      .in("notifications" / pathNotificationId)
      .summary("Mark single notification as read")
      .description("Mark single notification as read")
      .out(emptyOutput)
      .errorOut(errorOutputsFor(401, 404, 403))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user => notificationId =>
        arenaReadService.readNotification(notificationId, user)().handleErrorsOrOk
      }

    def markNotificationsAsRead: ServerEndpoint[Any, Eff] = endpoint.post
      .in("notifications")
      .summary("Mark your notifications as read")
      .description("Mark your notifications as read")
      .out(emptyOutput)
      .errorOut(errorOutputsFor(401, 403))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user => _ =>
        arenaReadService.readNotifications(user)().handleErrorsOrOk
      }

    def deleteSingleNotification: ServerEndpoint[Any, Eff] = endpoint.delete
      .in("notifications" / pathNotificationId)
      .summary("Delete single notification")
      .description("Delete single notification")
      .out(emptyOutput)
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user => notificationId =>
        arenaReadService.deleteNotification(notificationId, user)().handleErrorsOrOk
      }

    def deleteAllNotifications: ServerEndpoint[Any, Eff] = endpoint.delete
      .in("notifications")
      .summary("Delete all your notifications")
      .description("Delete all your notifications")
      .out(emptyOutput)
      .errorOut(errorOutputsFor(401, 403))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user => _ =>
        arenaReadService.deleteNotifications(user)().handleErrorsOrOk
      }

    def getPostInContext: ServerEndpoint[Any, Eff] = endpoint.get
      .in("posts" / pathPostId / "topic")
      .summary("Get a topic on the page where the post is")
      .description("Get a topic on the page where the post is")
      .out(jsonBody[TopicWithPosts])
      .errorOut(errorOutputsFor(401, 403, 404, 410))
      .in(queryPageSize)
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user =>
        { case (postId, pageSize) =>
          arenaReadService.getTopicByPostId(postId, user, pageSize)().handleErrorsOrOk
        }
      }

    def getByUsername: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Get user data by username")
      .description("Get user data by username")
      .in("user")
      .in(path[String]("username").description("Username of user"))
      .out(jsonBody[ArenaUser])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { _ => username =>
        userService.getArenaUserByUserName(username).handleErrorsOrOk
      }

    def listUsers: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("List users")
      .description("List users")
      .in("users")
      .in(queryPage)
      .in(queryPageSize)
      .out(jsonBody[PaginatedArenaUsers])
      .errorOut(errorOutputsFor(401, 403))
      .requireMyNDLAUser(requireArenaAdmin = true)
      .serverLogicPure { _ =>
        { case (page, pageSize) =>
          userService.getArenaUsersPaginated(page, pageSize)().handleErrorsOrOk
        }
      }

    override protected val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      getCategories,
      sortCategories,
      getCategory,
      getCategoryTopics,
      followCategory,
      unfollowCategory,
      getRecentTopics,
      followTopic,
      unfollowTopic,
      getTopic,
      postTopic,
      editTopic,
      deleteTopic,
      postPostToTopic,
      getPostInContext,
      editPost,
      deletePost,
      getFlags,
      flagPost,
      resolveFlag,
      postCategory,
      updateCategory,
      deleteCategory,
      getNotifications,
      markNotificationsAsRead,
      markSingleNotificationAsRead,
      deleteSingleNotification,
      deleteAllNotifications,
      getByUsername,
      listUsers
    )
  }

}
