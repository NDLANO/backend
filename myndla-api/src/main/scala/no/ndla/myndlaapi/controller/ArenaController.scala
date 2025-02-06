/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.controller

import cats.implicits.toTraverseOps
import io.circe.generic.auto.*
import no.ndla.common.model.api.myndla.{MyNDLAUserDTO, UpdatedMyNDLAUserDTO}
import no.ndla.common.model.domain.myndla.auth.AuthUtility
import no.ndla.myndlaapi.model.api.{ArenaUserDTO, PaginatedArenaUsersDTO}
import no.ndla.myndlaapi.MyNDLAAuthHelpers
import no.ndla.myndlaapi.model.arena.api.{
  CategoryDTO,
  CategorySortDTO,
  CategoryWithTopicsDTO,
  FlagDTO,
  NewCategoryDTO,
  NewFlagDTO,
  NewPostDTO,
  NewTopicDTO,
  PaginatedNewPostNotificationsDTO,
  PaginatedPostsDTO,
  PaginatedTopicsDTO,
  PostDTO,
  TopicDTO,
  TopicWithPostsDTO
}
import no.ndla.myndlaapi.service.{ArenaReadService, UserService}
import no.ndla.network.clients.FeideApiClient
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import no.ndla.network.tapir.TapirController
import no.ndla.network.tapir.auth.Permission.LEARNINGPATH_API_ADMIN
import no.ndla.network.tapir.auth.TokenUser
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.*

trait ArenaController {
  this: ErrorHandling & MyNDLAAuthHelpers & ArenaReadService & FeideApiClient & UserService & TapirController =>
  val arenaController: ArenaController

  class ArenaController extends TapirController {
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
      .in(query[CategorySortDTO]("sort").description("Sort categories").default(CategorySortDTO.ByRank))
      .out(jsonBody[List[CategoryDTO]])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user =>
        { case (followed, sort) =>
          arenaReadService.getCategories(user, followed, sort, None)()
        }
      }

    def getCategory: ServerEndpoint[Any, Eff] = endpoint.get
      .in("categories" / pathCategoryId)
      .summary("Get single category")
      .description("Get single category")
      .out(jsonBody[CategoryWithTopicsDTO])
      .in(queryPage)
      .in(queryPageSize)
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user =>
        { case (categoryId, page, pageSize) =>
          arenaReadService.getCategory(categoryId, page, pageSize, user)()
        }
      }

    def getCategoryTopics: ServerEndpoint[Any, Eff] = endpoint.get
      .in("categories" / pathCategoryId / "topics")
      .summary("Get topics for a category")
      .description("Get topics for a category")
      .out(jsonBody[PaginatedTopicsDTO])
      .in(queryPage)
      .in(queryPageSize)
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user =>
        { case (categoryId, page, pageSize) =>
          arenaReadService.getTopicsForCategory(categoryId, page, pageSize, user)()
        }
      }

    def sortCategories: ServerEndpoint[Any, Eff] = endpoint.put
      .in("categories" / "sort")
      .in(query[Option[Long]]("category-parent-id").description("Which parent to sort the categories for, if any"))
      .summary("Sort categories")
      .description("Sort categories")
      .in(jsonBody[List[Long]].description("List of category ids in the order they should be sorted"))
      .out(jsonBody[List[CategoryDTO]])
      .errorOut(errorOutputsFor(401, 403))
      .requireMyNDLAUser(requireArenaAdmin = true)
      .serverLogicPure { user =>
        { case (parentId, sortedIds) =>
          arenaReadService.sortCategories(parentId, sortedIds, user)
        }
      }

    def getTopic: ServerEndpoint[Any, Eff] = endpoint.get
      .in("topics" / pathTopicId)
      .summary("Get single topic")
      .description("Get single topic")
      .out(jsonBody[TopicWithPostsDTO])
      .errorOut(errorOutputsFor(401, 403, 404, 410))
      .in(queryPage)
      .in(queryPageSize)
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user =>
        { case (topicId, page, pageSize) =>
          arenaReadService.getTopic(topicId, user, page, pageSize)()
        }
      }

    def getRecentTopics: ServerEndpoint[Any, Eff] = endpoint.get
      .in("topics" / "recent")
      .summary("Get recent topics")
      .description("Get recent topics")
      .in(queryPage)
      .in(queryPageSize)
      .in(query[Option[Long]]("user-id").description("A users id to filter on"))
      .out(jsonBody[PaginatedTopicsDTO])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user =>
        { case (page, pageSize, ownerId) =>
          arenaReadService.getRecentTopics(page, pageSize, ownerId, user)()
        }
      }

    def followCategory: ServerEndpoint[Any, Eff] = endpoint.post
      .in("categories" / pathCategoryId / "follow")
      .summary("Follow category")
      .description("Follow category")
      .out(jsonBody[CategoryWithTopicsDTO])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user => categoryId =>
        arenaReadService.followCategory(categoryId, user)()
      }

    def unfollowCategory: ServerEndpoint[Any, Eff] = endpoint.post
      .in("categories" / pathCategoryId / "unfollow")
      .summary("Unfollow category")
      .description("Unfollow category")
      .out(jsonBody[CategoryWithTopicsDTO])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user => categoryId =>
        arenaReadService.unfollowCategory(categoryId, user)()
      }

    def followTopic: ServerEndpoint[Any, Eff] = endpoint.post
      .in("topics" / pathTopicId / "follow")
      .summary("Follow topic")
      .description("Follow topic")
      .out(jsonBody[TopicWithPostsDTO])
      .errorOut(errorOutputsFor(401, 403, 404, 410))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user => topicId =>
        arenaReadService.followTopic(topicId, user)()
      }

    def unfollowTopic: ServerEndpoint[Any, Eff] = endpoint.post
      .in("topics" / pathTopicId / "unfollow")
      .summary("Unfollow topic")
      .description("Unfollow topic")
      .out(jsonBody[TopicWithPostsDTO])
      .errorOut(errorOutputsFor(401, 403, 404, 410))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user => topicId =>
        arenaReadService.unfollowTopic(topicId, user)()
      }

    def upvotePost: ServerEndpoint[Any, Eff] = endpoint.put
      .in("posts" / pathPostId / "upvote")
      .summary("Upvote post")
      .description("Upvote post")
      .out(jsonBody[PostDTO])
      .errorOut(errorOutputsFor(401, 403, 404, 409))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user => postId =>
        arenaReadService.upvotePost(postId, user)()
      }

    def unUpvotePost: ServerEndpoint[Any, Eff] = endpoint.delete
      .in("posts" / pathPostId / "upvote")
      .summary("Remove upvote from post")
      .description("Remove a previously cast upvote from a post")
      .out(jsonBody[PostDTO])
      .errorOut(errorOutputsFor(401, 403, 404, 409))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user => postId =>
        arenaReadService.unUpvotePost(postId, user)()
      }

    def postTopic: ServerEndpoint[Any, Eff] = endpoint.post
      .in("categories" / pathCategoryId / "topics")
      .summary("Create new topic")
      .description("Create new topic")
      .in(jsonBody[NewTopicDTO])
      .out(statusCode(StatusCode.Created).and(jsonBody[TopicDTO]))
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user =>
        { case (categoryId, newTopic) =>
          arenaReadService.postTopic(categoryId, newTopic, user)
        }
      }

    def editTopic: ServerEndpoint[Any, Eff] = endpoint.put
      .in("topics" / pathTopicId)
      .summary("Edit a topic")
      .description("Edit a topic")
      .in(jsonBody[NewTopicDTO])
      .out(jsonBody[TopicDTO])
      .errorOut(errorOutputsFor(401, 403, 404, 410))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user =>
        { case (topicId, newTopic) =>
          arenaReadService.updateTopic(topicId, newTopic, user)()
        }
      }

    def postPostToTopic: ServerEndpoint[Any, Eff] = endpoint.post
      .in("topics" / pathTopicId / "posts")
      .summary("Add post to topic")
      .description("Add post to topic")
      .in(jsonBody[NewPostDTO])
      .out(statusCode(StatusCode.Created).and(jsonBody[PostDTO]))
      .errorOut(errorOutputsFor(401, 403, 404, 410))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user =>
        { case (topicId, newPost) =>
          arenaReadService.postPost(topicId, newPost, user)
        }
      }

    def postCategory: ServerEndpoint[Any, Eff] = endpoint.post
      .in("categories")
      .summary("Create new arena category")
      .description("Create new arena category")
      .in(jsonBody[NewCategoryDTO])
      .out(statusCode(StatusCode.Created).and(jsonBody[CategoryDTO]))
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArenaAdmin = true)
      .serverLogicPure { _ => newCategory =>
        arenaReadService.newCategory(newCategory)()
      }

    def updateCategory: ServerEndpoint[Any, Eff] = endpoint.put
      .in("categories" / pathCategoryId)
      .summary("Update arena category")
      .description("Update arena category")
      .in(jsonBody[NewCategoryDTO])
      .out(jsonBody[CategoryDTO])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArenaAdmin = true)
      .serverLogicPure { user =>
        { case (categoryId, newCategory) =>
          arenaReadService.updateCategory(categoryId, newCategory, user)()
        }
      }

    def editPost: ServerEndpoint[Any, Eff] = endpoint.put
      .in("posts" / pathPostId)
      .summary("Update arena post")
      .description("Update arena post")
      .in(jsonBody[NewPostDTO])
      .out(jsonBody[PostDTO])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user =>
        { case (postId, newPost) =>
          arenaReadService.updatePost(postId, newPost, user)()
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
        arenaReadService.deletePost(postId, user)()
      }

    def deleteTopic: ServerEndpoint[Any, Eff] = endpoint.delete
      .in("topics" / pathTopicId)
      .summary("Delete arena topic")
      .description("Delete arena topic")
      .out(emptyOutput)
      .errorOut(errorOutputsFor(401, 403, 404, 410))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user => topicId =>
        arenaReadService.deleteTopic(topicId, user)()
      }

    def deleteCategory: ServerEndpoint[Any, Eff] = endpoint.delete
      .in("categories" / pathCategoryId)
      .summary("Delete arena category")
      .description("Delete arena category")
      .out(emptyOutput)
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArenaAdmin = true)
      .serverLogicPure { user => categoryId =>
        arenaReadService.deleteCategory(categoryId, user)()
      }

    def flagPost: ServerEndpoint[Any, Eff] = endpoint.post
      .in("posts" / pathPostId / "flag")
      .summary("Flag arena post")
      .description("Flag arena post")
      .out(emptyOutput)
      .in(jsonBody[NewFlagDTO])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user =>
        { case (postId, newFlag) =>
          arenaReadService.flagPost(postId, user, newFlag)()
        }
      }

    def resolveFlag: ServerEndpoint[Any, Eff] = endpoint.put
      .in("flags" / pathFlagId)
      .summary("Toggle arena flag resolution status")
      .description("Toggle arena flag resolution status")
      .out(jsonBody[FlagDTO])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArenaAdmin = true)
      .serverLogicPure { _ => flagId =>
        arenaReadService.resolveFlag(flagId)()
      }

    def getFlags: ServerEndpoint[Any, Eff] = endpoint.get
      .in("flags")
      .summary("List flagged posts")
      .description("List flagged posts")
      .in(queryPage)
      .in(queryPageSize)
      .out(jsonBody[PaginatedPostsDTO])
      .errorOut(errorOutputsFor(401, 403))
      .requireMyNDLAUser(requireArenaAdmin = true)
      .serverLogicPure { user =>
        { case (page, pageSize) =>
          arenaReadService.getFlaggedPosts(page, pageSize, user)()
        }
      }

    def getNotifications: ServerEndpoint[Any, Eff] = endpoint.get
      .in("notifications")
      .summary("Get your notifications")
      .description("Get your notifications")
      .in(queryPage)
      .in(queryPageSize)
      .out(jsonBody[PaginatedNewPostNotificationsDTO])
      .errorOut(errorOutputsFor(401, 403))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user =>
        { case (page, pageSize) =>
          arenaReadService.getNotifications(user, page, pageSize)()
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
        arenaReadService.readNotification(notificationId, user)()
      }

    def markNotificationsAsRead: ServerEndpoint[Any, Eff] = endpoint.post
      .in("notifications")
      .summary("Mark your notifications as read")
      .description("Mark your notifications as read")
      .out(emptyOutput)
      .errorOut(errorOutputsFor(401, 403))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user => _ =>
        arenaReadService.readNotifications(user)()
      }

    def deleteSingleNotification: ServerEndpoint[Any, Eff] = endpoint.delete
      .in("notifications" / pathNotificationId)
      .summary("Delete single notification")
      .description("Delete single notification")
      .out(emptyOutput)
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user => notificationId =>
        arenaReadService.deleteNotification(notificationId, user)()
      }

    def deleteAllNotifications: ServerEndpoint[Any, Eff] = endpoint.delete
      .in("notifications")
      .summary("Delete all your notifications")
      .description("Delete all your notifications")
      .out(emptyOutput)
      .errorOut(errorOutputsFor(401, 403))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user => _ =>
        arenaReadService.deleteNotifications(user)()
      }

    def getPostInContext: ServerEndpoint[Any, Eff] = endpoint.get
      .in("posts" / pathPostId / "topic")
      .summary("Get a topic on the page where the post is")
      .description("Get a topic on the page where the post is")
      .out(jsonBody[TopicWithPostsDTO])
      .errorOut(errorOutputsFor(401, 403, 404, 410))
      .in(queryPageSize)
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { user =>
        { case (postId, pageSize) =>
          arenaReadService.getTopicByPostId(postId, user, pageSize)()
        }
      }

    def getByUsername: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Get user data by username")
      .description("Get user data by username")
      .in("user")
      .in(path[String]("username").description("Username of user"))
      .out(jsonBody[ArenaUserDTO])
      .errorOut(errorOutputsFor(401, 403, 404))
      .requireMyNDLAUser(requireArena = true)
      .serverLogicPure { _ => username =>
        userService.getArenaUserByUserName(username)
      }

    def listUsers: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("List users")
      .description("List users")
      .in("users")
      .in(queryPage)
      .in(queryPageSize)
      .in(query[Boolean]("filter-teachers").description("Whether to filter teachers or not").default(false))
      .in(query[Option[String]]("query").description("Search query to match against username or display name"))
      .out(jsonBody[PaginatedArenaUsersDTO])
      .errorOut(errorOutputsFor(401, 403))
      .requireMyNDLAUser(requireArenaAdmin = true)
      .serverLogicPure { _ =>
        { case (page, pageSize, filterTeachers, query) =>
          userService
            .getArenaUsersPaginated(
              page,
              pageSize,
              filterTeachers,
              query
            )()

        }
      }

    def adminUpdateMyNDLAUser: ServerEndpoint[Any, Eff] = endpoint.patch
      .summary("Update some one elses user data")
      .description("Update some one elses user data")
      .in("users" / path[Long]("user-id").description("UserID of user to update"))
      .in(jsonBody[UpdatedMyNDLAUserDTO])
      .out(jsonBody[MyNDLAUserDTO])
      .securityIn(TokenUser.oauth2Input(Seq.empty))
      .securityIn(AuthUtility.feideOauth())
      .errorOut(errorOutputsFor(401, 403, 404))
      .serverSecurityLogicPure { case (tokenUser, feideToken) =>
        val arenaUser = feideToken.traverse(token => userService.getArenaEnabledUser(Some(token))).toOption.flatten
        if (tokenUser.hasPermission(LEARNINGPATH_API_ADMIN) || arenaUser.exists(_.isAdmin)) {
          Right((tokenUser, arenaUser))
        } else Left(ErrorHelpers.forbidden)
      }
      .serverLogicPure {
        case (tokenUser, myndlaUser) => { case (userId, updatedMyNdlaUser) =>
          userService.adminUpdateMyNDLAUserData(userId, updatedMyNdlaUser, tokenUser, myndlaUser)()
        }
      }

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
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
      listUsers,
      adminUpdateMyNDLAUser,
      upvotePost,
      unUpvotePost
    )
  }

}
