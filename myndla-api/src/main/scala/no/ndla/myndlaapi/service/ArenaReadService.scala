/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.service

import cats.implicits._
import no.ndla.common.Clock
import no.ndla.common.implicits._
import no.ndla.common.errors.{AccessDeniedException, NotFoundException, ValidationException}
import no.ndla.common.implicits.OptionImplicit
import no.ndla.network.clients.FeideApiClient
import no.ndla.myndlaapi.model.arena.{api, domain}
import no.ndla.myndlaapi.model.arena.api.{Category, CategorySort, NewCategory, NewPost, NewTopic}
import no.ndla.myndlaapi.model.arena.domain.{MissingPostException, TopicGoneException}
import no.ndla.myndlaapi.model.arena.domain.database.{CompiledPost, CompiledTopic}
import no.ndla.myndlaapi.model.domain.MyNDLAUser
import no.ndla.myndlaapi.repository.{ArenaRepository, FolderRepository, UserRepository}
import no.ndla.network.model.FeideAccessToken
import scalikejdbc.{AutoSession, DBSession, ReadOnlyAutoSession}

import scala.util.{Failure, Success, Try}

trait ArenaReadService {
  this: FeideApiClient
    with ArenaRepository
    with ConverterService
    with UserService
    with Clock
    with ConfigService
    with FolderRepository
    with UserRepository =>
  val arenaReadService: ArenaReadService

  class ArenaReadService {
    def sortCategories(parentId: Option[Long], sortedIds: List[Long], user: MyNDLAUser): Try[List[api.Category]] =
      arenaRepository.rollbackOnFailure { session =>
        for {
          existingCategoryIds <- arenaRepository.getAllCategoryIds(parentId)(session)
          _ <-
            if (existingCategoryIds.sorted != sortedIds.sorted) {
              Failure(ValidationException("body", "Sorted ids must contain every existing category id"))
            } else { Success(()) }
          _ <- arenaRepository.sortCategories(sortedIds)(session)
          categories <- getCategories(
            user,
            filterFollowed = false,
            sort = CategorySort.ByRank,
            parentCategoryId = None
          )(session)
        } yield categories
      }

    def getTopicByPostId(
        postId: Long,
        requester: MyNDLAUser,
        pageSize: Long
    )(session: DBSession = ReadOnlyAutoSession): Try[api.TopicWithPosts] = {
      for {
        maybePost <- arenaRepository.getPost(postId)(session)
        (post, _) <- maybePost.toTry(NotFoundException(s"Could not find post with id $postId"))
        topicPage <- arenaRepository.getTopicPageByPostId(post.topic_id, postId, pageSize)(session)
        topic <- getTopic(
          topicId = post.topic_id,
          user = requester,
          page = topicPage,
          pageSize = pageSize
        )(session)
      } yield topic
    }

    def getFlaggedPosts(
        page: Long,
        pageSize: Long,
        requester: MyNDLAUser
    )(session: DBSession = ReadOnlyAutoSession): Try[api.PaginatedPosts] = {
      val offset = (page - 1) * pageSize
      for {
        posts     <- arenaRepository.getFlaggedPosts(offset, pageSize)(session)
        postCount <- arenaRepository.getFlaggedPostsCount(session)
        apiPosts = posts.map(compiledPost => {
          val replies = getRepliesForPost(compiledPost.post.id, requester)(session).?
          converterService.toApiPost(compiledPost, requester, replies)
        })
      } yield api.PaginatedPosts(
        items = apiPosts,
        totalCount = postCount,
        pageSize = pageSize,
        page = page
      )
    }

    def readNotifications(user: MyNDLAUser)(implicit session: DBSession = AutoSession): Try[Unit] =
      arenaRepository.readNotifications(user.id)(session)

    def deleteNotification(notificationId: Long, user: MyNDLAUser)(implicit
        session: DBSession = AutoSession
    ): Try[Unit] =
      arenaRepository.deleteNotification(notificationId, user.id)(session)

    def deleteNotifications(user: MyNDLAUser)(implicit session: DBSession = AutoSession): Try[Unit] =
      arenaRepository.deleteNotifications(user.id)(session)

    def readNotification(notificationId: Long, user: MyNDLAUser)(implicit session: DBSession = AutoSession): Try[Unit] =
      arenaRepository.readNotification(notificationId, user.id)(session)

    def getNotifications(user: MyNDLAUser, page: Long, pageSize: Long)(implicit
        session: DBSession = ReadOnlyAutoSession
    ): Try[api.PaginatedNewPostNotifications] = {
      val offset = (page - 1) * pageSize
      for {
        compiledNotifications <- arenaRepository.getNotifications(user, offset, pageSize)(session)
        notificationsCount    <- arenaRepository.notificationsCount(user.id)(session)
        apiNotifications = compiledNotifications.map { notification =>
          {
            val replies = getRepliesForPost(notification.post.post.id, user)(session).?

            api.NewPostNotification(
              id = notification.notification.id,
              isRead = notification.notification.is_read,
              topicTitle = notification.topic.title,
              topicId = notification.topic.id,
              post = converterService.toApiPost(notification.post, user, replies),
              notificationTime = notification.notification.notification_time
            )
          }
        }
      } yield api.PaginatedNewPostNotifications(
        items = apiNotifications,
        totalCount = notificationsCount,
        pageSize = pageSize,
        page = page
      )
    }

    def resolveFlag(flagId: Long)(session: DBSession = AutoSession): Try[api.Flag] = for {
      maybeFlag <- arenaRepository.getFlag(flagId)(session)
      flag      <- maybeFlag.toTry(NotFoundException(s"Could not find flag with id $flagId"))
      updated   <- toggleFlagResolution(flag.flag)(session)
    } yield converterService.toApiFlag(flag.copy(flag = updated))

    def toggleFlagResolution(flag: domain.Flag)(session: DBSession): Try[domain.Flag] = {
      if (flag.resolved.isDefined) {
        arenaRepository.unresolveFlag(flag.id)(session).map(_ => flag.copy(resolved = None))
      } else {
        val resolveTime = clock.now()
        arenaRepository.resolveFlag(flag.id, resolveTime)(session).map(_ => flag.copy(resolved = Some(resolveTime)))
      }
    }

    def flagPost(postId: Long, user: MyNDLAUser, newFlag: api.NewFlag)(session: DBSession = AutoSession): Try[Unit] =
      for {
        maybePost <- arenaRepository.getPost(postId)(session)
        _         <- maybePost.toTry(NotFoundException(s"Could not find post with id $postId"))
        created = clock.now()
        _ <- arenaRepository.flagPost(user, postId, newFlag.reason, created)(session)
      } yield ()

    def deleteCategory(categoryId: Long, user: MyNDLAUser)(session: DBSession = AutoSession): Try[Unit] = for {
      _          <- if (user.isAdmin) Success(()) else Failure(AccessDeniedException.forbidden)
      maybeTopic <- arenaRepository.getCategory(categoryId, includeHidden = true)(session)
      _          <- maybeTopic.toTry(NotFoundException(s"Could not find category with id $categoryId"))
      _          <- arenaRepository.deleteCategory(categoryId)(session)
    } yield ()

    def deleteTopic(topicId: Long, user: MyNDLAUser)(session: DBSession = AutoSession): Try[Unit] = for {
      topic <- getCompiledTopic(topicId, user)(session)
      _     <- failIfEditDisallowed(topic.topic, user)
      _     <- arenaRepository.deleteTopic(topicId)(session)
    } yield ()

    def deletePost(postId: Long, user: MyNDLAUser)(session: DBSession = AutoSession): Try[Unit] = for {
      maybePost <- arenaRepository.getPost(postId)(session)
      (post, _) <- maybePost.toTry(NotFoundException(s"Could not find post with id $postId"))
      _         <- failIfEditDisallowed(post, user)
      _         <- arenaRepository.deletePost(postId)(session)
    } yield ()

    def getTopicsForCategory(categoryId: Long, page: Long, pageSize: Long, requester: MyNDLAUser)(
        session: DBSession = ReadOnlyAutoSession
    ): Try[api.PaginatedTopics] = {
      val offset = (page - 1) * pageSize
      for {
        maybeCategory <- arenaRepository.getCategory(categoryId, includeHidden = false)(session)
        _             <- maybeCategory.toTry(NotFoundException(s"Could not find category with id $categoryId"))
        topics        <- arenaRepository.getTopicsForCategory(categoryId, offset, pageSize, requester)(session)
        topicsCount   <- arenaRepository.getTopicCountForCategory(categoryId)(session)
      } yield api.PaginatedTopics(
        items = topics.map { topic => converterService.toApiTopic(topic) },
        totalCount = topicsCount,
        pageSize = pageSize,
        page = page
      )
    }

    def getRecentTopics(page: Long, pageSize: Long, ownerId: Option[Long], requester: MyNDLAUser)(
        session: DBSession = ReadOnlyAutoSession
    ): Try[api.PaginatedTopics] = {
      val offset = (page - 1) * pageSize

      val topicsT = ownerId
        .map(id => arenaRepository.getUserTopicsPaginated(id, offset, pageSize, requester)(session))
        .getOrElse(arenaRepository.getTopicsPaginated(offset, pageSize, requester)(session))

      for {
        (topics, topicsCount) <- topicsT
        apiTopics = topics.map { topic => converterService.toApiTopic(topic) }
      } yield api.PaginatedTopics(
        items = apiTopics,
        totalCount = topicsCount,
        pageSize = pageSize,
        page = page
      )
    }

    def updateTopic(topicId: Long, newTopic: NewTopic, user: MyNDLAUser)(
        session: DBSession = AutoSession
    ): Try[api.Topic] = {
      val updatedTime = clock.now()
      for {
        topic <- getCompiledTopic(topicId, user)(session)
        posts <- arenaRepository.getPostsForTopic(topicId, 0, 10)(session)
        _     <- failIfEditDisallowed(topic.topic, user)
        updatedTopic <- arenaRepository.updateTopic(
          topicId = topicId,
          title = newTopic.title,
          updated = updatedTime,
          locked = if (user.isAdmin) newTopic.isLocked.getOrElse(false) else topic.topic.locked,
          pinned = if (user.isAdmin) newTopic.isPinned.getOrElse(false) else topic.topic.pinned
        )(session)
        mainPostId <- posts.headOption
          .map(_.post.id)
          .toTry(MissingPostException("Could not find main post for topic"))
        _ <- arenaRepository.updatePost(mainPostId, newTopic.initialPost.content, updatedTime)(session)
        compiledTopic = topic.copy(topic = updatedTopic)
      } yield converterService.toApiTopic(compiledTopic)
    }

    def updatePost(postId: Long, newPost: NewPost, user: MyNDLAUser)(
        session: DBSession = AutoSession
    ): Try[api.Post] = {
      val updatedTime = clock.now()
      for {
        maybePost     <- arenaRepository.getPost(postId)(session)
        (post, owner) <- maybePost.toTry(NotFoundException(s"Could not find post with id $postId"))
        _             <- failIfEditDisallowed(post, user)
        updatedPost   <- arenaRepository.updatePost(postId, newPost.content, updatedTime)(session)
        flags         <- arenaRepository.getFlagsForPost(postId)(session)
        replies       <- getRepliesForPost(post.id, user)(session)
        compiledPost = CompiledPost(updatedPost, owner, flags)
      } yield converterService.toApiPost(compiledPost, user, replies)
    }

    def getRepliesForPost(parentPostId: Long, requester: MyNDLAUser)(
        session: DBSession = AutoSession
    ): Try[List[api.Post]] = Try {
      val replies = arenaRepository.getReplies(parentPostId)(session).?
      replies.map(r => {
        val replyReplies = getRepliesForPost(r.post.id, requester)(session).?
        converterService.toApiPost(r, requester, replyReplies)
      })
    }

    private def failIfEditDisallowed(owned: domain.Owned, user: MyNDLAUser): Try[Unit] = {
      if (user.isAdmin) return Success(())

      val isOwner = owned.ownerId.contains(user.id)
      if (isOwner && !owned.locked) return Success(())

      Failure(AccessDeniedException.forbidden)
    }

    def newCategory(newCategory: NewCategory)(session: DBSession = AutoSession): Try[Category] = {
      val toInsert = domain.InsertCategory(
        newCategory.title,
        newCategory.description,
        newCategory.visible,
        newCategory.parentCategoryId
      )
      arenaRepository.insertCategory(toInsert)(session).map { inserted =>
        converterService.toApiCategory(inserted, 0, 0, isFollowing = false, List.empty, List.empty)

      }
    }

    def getNewRank(existing: api.CategoryWithTopics, newParentId: Option[Long])(session: DBSession): Try[Int] = {
      val parentHasChanged = existing.parentCategoryId.exists(newParentId.contains)
      if (parentHasChanged) {
        arenaRepository.getNextParentRank(newParentId)(session)
      } else Success(existing.rank)
    }

    def updateCategory(categoryId: Long, newCategory: NewCategory, user: MyNDLAUser)(
        session: DBSession = AutoSession
    ): Try[Category] = {
      val toInsert = domain.InsertCategory(
        newCategory.title,
        newCategory.description,
        newCategory.visible,
        newCategory.parentCategoryId
      )
      for {
        existing      <- getCategory(categoryId, 0, 0, user)(session)
        newRank       <- getNewRank(existing, toInsert.parentCategoryId)(session)
        updated       <- arenaRepository.updateCategory(categoryId, toInsert, newRank)(session)
        following     <- arenaRepository.getCategoryFollowing(categoryId, user.id)(session)
        subcategories <- getCategories(user, filterFollowed = false, CategorySort.ByRank, categoryId.some)(session)
        breadcrumbs   <- arenaRepository.getBreadcrumbs(existing.id)(session)
      } yield converterService.toApiCategory(
        updated,
        existing.topicCount,
        existing.postCount,
        following.isDefined,
        subcategories,
        breadcrumbs
      )
    }

    def postTopic(categoryId: Long, newTopic: NewTopic, user: MyNDLAUser): Try[api.Topic] = {
      arenaRepository.withSession { session =>
        val created = clock.now()
        for {
          _ <- getCategory(categoryId, 0, 0, user)(session)
          topic <- arenaRepository.insertTopic(
            categoryId = categoryId,
            title = newTopic.title,
            ownerId = user.id,
            created = created,
            updated = created,
            locked = if (user.isAdmin) newTopic.isLocked.getOrElse(false) else false,
            pinned = if (user.isAdmin) newTopic.isPinned.getOrElse(false) else false
          )(session)
          _ <- followTopic(topic.id, user)(session)
          _ <- arenaRepository.postPost(topic.id, newTopic.initialPost.content, user.id, created, created, None)(
            session
          )
          compiledTopic = CompiledTopic(topic, Some(user), 1, isFollowing = true)
        } yield converterService.toApiTopic(compiledTopic)
      }
    }

    private def failIfPostDisallowed(topic: CompiledTopic, user: MyNDLAUser): Try[Unit] = {
      if (user.isAdmin) return Success(())
      if (topic.topic.locked) return Failure(AccessDeniedException.forbidden)
      Success(())
    }

    def postPost(topicId: Long, newPost: NewPost, user: MyNDLAUser): Try[api.Post] =
      arenaRepository.withSession { session =>
        val created = clock.now()
        for {
          topic <- getCompiledTopic(topicId, user)(session)
          _     <- failIfPostDisallowed(topic, user)
          newPost <- arenaRepository.postPost(topicId, newPost.content, user.id, created, created, newPost.toPostId)(
            session
          )
          _ <- generateNewPostNotifications(topic, newPost)(session)
          _ <- followTopic(topicId, user)(session)
          compiledPost = CompiledPost(newPost, Some(user), List.empty)
        } yield converterService.toApiPost(compiledPost, user, List.empty)
      }

    def generateNewPostNotifications(topic: CompiledTopic, newPost: domain.Post)(
        session: DBSession
    ): Try[List[domain.Notification]] = {
      val notificationTime = clock.now()
      getFollowers(topic.topic.id)(session)
        .flatMap { followers =>
          followers.traverse { follower =>
            if (newPost.ownerId.contains(follower.id)) Success(None)
            else
              arenaRepository
                .insertNotification(
                  follower.id,
                  newPost.id,
                  topic.topic.id,
                  notificationTime
                )(session)
                .map(Some(_))
          }
        }
        .map(_.flatten)
    }

    def getFollowers(topicId: Long)(session: DBSession): Try[List[MyNDLAUser]] = {
      arenaRepository.getTopicFollowers(topicId)(session)
    }

    def getCategory(categoryId: Long, page: Long, pageSize: Long, requester: MyNDLAUser)(
        session: DBSession = ReadOnlyAutoSession
    ): Try[api.CategoryWithTopics] = {
      val offset = (page - 1) * pageSize
      for {
        maybeCategory <- arenaRepository.getCategory(categoryId, includeHidden = requester.isAdmin)(session)
        category      <- maybeCategory.toTry(NotFoundException(s"Could not find category with id $categoryId"))
        topics        <- arenaRepository.getTopicsForCategory(categoryId, offset, pageSize, requester)(session)
        subcats     <- getCategories(requester, filterFollowed = false, CategorySort.ByRank, category.id.some)(session)
        topicsCount <- arenaRepository.getTopicCountForCategory(categoryId)(session)
        breadcrumb  <- arenaRepository.getBreadcrumbs(categoryId)(session)
        postsCount  <- arenaRepository.getPostCountForCategory(categoryId)(session)
        following   <- arenaRepository.getCategoryFollowing(categoryId, requester.id)(session)
        tt = topics.map(topic => converterService.toApiTopic(topic))
      } yield api.CategoryWithTopics(
        id = categoryId,
        title = category.title,
        description = category.description,
        topicCount = topicsCount,
        postCount = postsCount,
        topics = tt,
        topicPageSize = pageSize,
        topicPage = page,
        isFollowing = following.isDefined,
        visible = category.visible,
        rank = category.rank,
        categoryCount = subcats.size.toLong,
        subcategories = subcats,
        parentCategoryId = category.parentCategoryId,
        breadcrumbs = breadcrumb
      )
    }

    private def getCompiledTopic(topicId: Long, user: MyNDLAUser)(session: DBSession): Try[CompiledTopic] = {
      arenaRepository.getTopic(topicId, user)(session) match {
        case Failure(ex) => Failure(ex)
        case Success(Some(topic)) if topic.topic.deleted.isDefined =>
          Failure(TopicGoneException(s"Topic with id $topicId is gone"))
        case Success(Some(topic)) => Success(topic)
        case Success(None)        => Failure(NotFoundException(s"Could not find topic with id $topicId"))
      }
    }

    def getTopic(topicId: Long, user: MyNDLAUser, page: Long, pageSize: Long)(
        session: DBSession = AutoSession
    ): Try[api.TopicWithPosts] = {
      val offset = (page - 1) * pageSize
      for {
        topic    <- getCompiledTopic(topicId, user)(session)
        posts    <- arenaRepository.getPostsForTopic(topicId, offset, pageSize)(session)
        _        <- readNotification(user, topicId, posts)(session)
        apiPosts <- getRepliesForPosts(posts.filter(_.post.toPostId.isEmpty), user)(session)
      } yield converterService.toApiTopicWithPosts(
        compiledTopic = topic,
        page = page,
        pageSize = pageSize,
        posts = apiPosts
      )
    }

    def getRepliesForPosts(posts: List[CompiledPost], requester: MyNDLAUser)(
        session: DBSession = AutoSession
    ): Try[List[api.Post]] = Try {
      posts.map(post => {
        val replies = getRepliesForPost(post.post.id, requester)(session).?
        converterService.toApiPost(post, requester, replies)
      })
    }

    def readNotification(user: MyNDLAUser, topicId: Long, posts: List[CompiledPost])(session: DBSession): Try[Unit] = {
      val postIds       = posts.map(_.post.id).toSet
      val notifications = arenaRepository.getNotificationsForTopic(user, topicId)(session).?
      val toRead        = notifications.filter(not => postIds.contains(not.post.post.id))
      val read = toRead.traverse(not => arenaRepository.readNotification(not.notification.id, user.id)(session))
      read.map(_ => ())
    }

    def followTopic(topicId: Long, user: MyNDLAUser)(session: DBSession = AutoSession): Try[api.TopicWithPosts] = {
      for {
        apiTopic  <- getTopic(topicId, user, 1, 10)(session)
        following <- arenaRepository.getTopicFollowing(topicId, user.id)(session)
        _         <- if (following.isEmpty) arenaRepository.followTopic(topicId, user.id)(session) else Success(())
      } yield apiTopic
    }

    def unfollowTopic(topicId: Long, user: MyNDLAUser)(session: DBSession = AutoSession): Try[api.TopicWithPosts] = {
      for {
        apiTopic  <- getTopic(topicId, user, 0, 0)(session)
        following <- arenaRepository.getTopicFollowing(topicId, user.id)(session)
        _         <- if (following.isDefined) arenaRepository.unfollowTopic(topicId, user.id)(session) else Success(())
      } yield apiTopic
    }

    def followCategory(categoryId: Long, user: MyNDLAUser)(
        session: DBSession = AutoSession
    ): Try[api.CategoryWithTopics] = {
      for {
        apiCategory <- getCategory(categoryId, 1, 10, user)(session)
        following   <- arenaRepository.getCategoryFollowing(categoryId, user.id)(session)
        _ <- if (following.isEmpty) arenaRepository.followCategory(categoryId, user.id)(session) else Success(())
      } yield apiCategory
    }

    def unfollowCategory(categoryId: Long, user: MyNDLAUser)(
        session: DBSession = AutoSession
    ): Try[api.CategoryWithTopics] = {
      for {
        apiTopic  <- getCategory(categoryId, 1, 10, user)(session)
        following <- arenaRepository.getCategoryFollowing(categoryId, user.id)(session)
        _ <- if (following.isDefined) arenaRepository.unfollowCategory(categoryId, user.id)(session) else Success(())
      } yield apiTopic
    }

    def getCategories(
        requester: MyNDLAUser,
        filterFollowed: Boolean,
        sort: CategorySort,
        parentCategoryId: Option[Long]
    )(session: DBSession = ReadOnlyAutoSession): Try[List[api.Category]] =
      arenaRepository
        .getCategories(requester, filterFollowed, sort, parentCategoryId)(session)
        .flatMap(categories => {
          categories.traverse(category => {
            for {
              postCount     <- arenaRepository.getPostCountForCategory(category.id)(session)
              topicCount    <- arenaRepository.getTopicCountForCategory(category.id)(session)
              subcategories <- getCategories(requester, filterFollowed, sort, Some(category.id))(session)
              following     <- arenaRepository.getCategoryFollowing(category.id, requester.id)(session)
              breadcrumbs   <- arenaRepository.getBreadcrumbs(category.id)(session)
            } yield converterService.toApiCategory(
              category,
              topicCount,
              postCount,
              following.isDefined,
              subcategories,
              breadcrumbs
            )
          })
        })

    def deleteAllUserData(feideAccessToken: Option[FeideAccessToken]): Try[Unit] =
      arenaRepository.rollbackOnFailure(session => {
        for {
          feideId <- feideApiClient.getFeideID(feideAccessToken)
          user    <- userService.getOrCreateMyNDLAUserIfNotExist(feideId, feideAccessToken, List.empty)(session)
          _       <- arenaRepository.disconnectPostsByUser(user.id)(session)
          _       <- arenaRepository.disconnectTopicsByUser(user.id)(session)
          _       <- arenaRepository.disconnectFlagsByUser(user.id)(session)
          _       <- folderRepository.deleteAllUserFolders(feideId)(session)
          _       <- folderRepository.deleteAllUserResources(feideId)(session)
          _       <- userRepository.deleteUser(feideId)(session)
        } yield ()
      })
  }
}
