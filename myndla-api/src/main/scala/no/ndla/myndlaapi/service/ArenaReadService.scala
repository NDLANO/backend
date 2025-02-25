/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.service

import cats.implicits.*
import no.ndla.common.Clock
import no.ndla.common.implicits.*
import no.ndla.common.errors.{AccessDeniedException, InvalidStateException, NotFoundException, ValidationException}
import no.ndla.common.implicits.OptionImplicit
import no.ndla.common.model.domain.myndla.MyNDLAUser
import no.ndla.database.DBUtility
import no.ndla.myndlaapi.integration.nodebb.NodeBBClient
import no.ndla.network.clients.FeideApiClient
import no.ndla.myndlaapi.model.arena.{api, domain}
import no.ndla.myndlaapi.model.arena.api.{CategoryDTO, CategorySortDTO, NewCategoryDTO, NewPostDTO, NewTopicDTO}
import no.ndla.myndlaapi.model.arena.domain.{MissingPostException, TopicGoneException}
import no.ndla.myndlaapi.model.arena.domain.database.{CompiledPost, CompiledTopic}
import no.ndla.myndlaapi.repository.{ArenaRepository, FolderRepository, UserRepository}
import no.ndla.network.model.FeideAccessToken
import scalikejdbc.{AutoSession, DBSession, ReadOnlyAutoSession}

import scala.util.{Failure, Success, Try}

trait ArenaReadService {
  this: FeideApiClient & ArenaRepository & ConverterService & UserService & Clock & ConfigService & FolderRepository &
    UserRepository & NodeBBClient & DBUtility =>
  val arenaReadService: ArenaReadService

  class ArenaReadService {
    def sortCategories(parentId: Option[Long], sortedIds: List[Long], user: MyNDLAUser): Try[List[api.CategoryDTO]] =
      DBUtil.rollbackOnFailure { session =>
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
            sort = CategorySortDTO.ByRank,
            parentCategoryId = None
          )(session)
        } yield categories
      }

    def getTopicByPostId(
        postId: Long,
        requester: MyNDLAUser,
        pageSize: Long
    )(session: DBSession = AutoSession): Try[api.TopicWithPostsDTO] = {
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
    )(session: DBSession = ReadOnlyAutoSession): Try[api.PaginatedPostsDTO] = {
      val offset = (page - 1) * pageSize
      for {
        posts     <- arenaRepository.getFlaggedPosts(offset, pageSize, requester)(session)
        postCount <- arenaRepository.getFlaggedPostsCount(session)
        apiPosts = posts.map(compiledPost => {
          val replies = getRepliesForPost(compiledPost.post.id, requester)(session).?
          converterService.toApiPost(compiledPost, requester, replies)
        })
      } yield api.PaginatedPostsDTO(
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
    ): Try[api.PaginatedNewPostNotificationsDTO] = {
      val offset = (page - 1) * pageSize
      for {
        compiledNotifications <- arenaRepository.getNotifications(user, offset, pageSize)(session)
        notificationsCount    <- arenaRepository.notificationsCount(user.id)(session)
        apiNotifications = compiledNotifications.map { notification =>
          {
            val replies = getRepliesForPost(notification.post.post.id, user)(session).?

            api.NewPostNotificationDTO(
              id = notification.notification.id,
              isRead = notification.notification.is_read,
              topicTitle = notification.topic.title,
              topicId = notification.topic.id,
              post = converterService.toApiPost(notification.post, user, replies),
              notificationTime = notification.notification.notification_time
            )
          }
        }
      } yield api.PaginatedNewPostNotificationsDTO(
        items = apiNotifications,
        totalCount = notificationsCount,
        pageSize = pageSize,
        page = page
      )
    }

    def resolveFlag(flagId: Long)(session: DBSession = AutoSession): Try[api.FlagDTO] = for {
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

    def flagPost(postId: Long, user: MyNDLAUser, newFlag: api.NewFlagDTO)(session: DBSession = AutoSession): Try[Unit] =
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
    ): Try[api.PaginatedTopicsDTO] = {
      val offset = (page - 1) * pageSize
      for {
        maybeCategory <- arenaRepository.getCategory(categoryId, includeHidden = false)(session)
        _             <- maybeCategory.toTry(NotFoundException(s"Could not find category with id $categoryId"))
        topics        <- arenaRepository.getTopicsForCategory(categoryId, offset, pageSize, requester)(session)
        topicsCount   <- arenaRepository.getTopicCountForCategory(categoryId)(session)
      } yield api.PaginatedTopicsDTO(
        items = topics.map { topic => converterService.toApiTopic(topic) },
        totalCount = topicsCount,
        pageSize = pageSize,
        page = page
      )
    }

    def getRecentTopics(page: Long, pageSize: Long, ownerId: Option[Long], requester: MyNDLAUser)(
        session: DBSession = ReadOnlyAutoSession
    ): Try[api.PaginatedTopicsDTO] = {
      val offset = (page - 1) * pageSize

      val topicsT = ownerId
        .map(id => arenaRepository.getUserTopicsPaginated(id, offset, pageSize, requester)(session))
        .getOrElse(arenaRepository.getTopicsPaginated(offset, pageSize, requester)(session))

      for {
        (topics, topicsCount) <- topicsT
        apiTopics = topics.map { topic => converterService.toApiTopic(topic) }
      } yield api.PaginatedTopicsDTO(
        items = apiTopics,
        totalCount = topicsCount,
        pageSize = pageSize,
        page = page
      )
    }

    def updateTopic(topicId: Long, newTopic: NewTopicDTO, user: MyNDLAUser)(
        session: DBSession = AutoSession
    ): Try[api.TopicDTO] = {
      val updatedTime = clock.now()
      for {
        topic <- getCompiledTopic(topicId, user)(session)
        posts <- arenaRepository.getPostsForTopic(topicId, 0, 10, user)(session)
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

    def updatePost(postId: Long, newPost: NewPostDTO, user: MyNDLAUser)(
        session: DBSession = AutoSession
    ): Try[api.PostDTO] = {
      val updatedTime = clock.now()
      for {
        maybePost     <- arenaRepository.getPost(postId)(session)
        (post, owner) <- maybePost.toTry(NotFoundException(s"Could not find post with id $postId"))
        _             <- failIfEditDisallowed(post, user)
        updatedPost   <- arenaRepository.updatePost(postId, newPost.content, updatedTime)(session)
        flags         <- arenaRepository.getFlagsForPost(postId)(session)
        replies       <- getRepliesForPost(post.id, user)(session)
        upvotes       <- arenaRepository.getUpvotesForPost(postId)(session)
        upvoted       <- arenaRepository.getUpvoted(postId, user.id)(session)
        compiledPost = CompiledPost(updatedPost, owner, flags, upvotes.length, upvoted.isDefined)
      } yield converterService.toApiPost(compiledPost, user, replies)
    }

    def getRepliesForPost(parentPostId: Long, requester: MyNDLAUser)(
        session: DBSession = AutoSession
    ): Try[List[api.PostDTO]] = Try {
      val replies = arenaRepository.getReplies(parentPostId, requester)(session).?
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

    def newCategory(newCategory: NewCategoryDTO)(session: DBSession = AutoSession): Try[CategoryDTO] = {
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

    def getNewRank(existing: api.CategoryWithTopicsDTO, newParentId: Option[Long])(session: DBSession): Try[Int] = {
      val parentHasChanged = existing.parentCategoryId.exists(newParentId.contains)
      if (parentHasChanged) {
        arenaRepository.getNextParentRank(newParentId)(session)
      } else Success(existing.rank)
    }

    private def validateParentCategory(category: api.CategoryTypeDTO, maybeNewParentId: Option[Long], user: MyNDLAUser)(
        session: DBSession
    ): Try[Unit] = {
      maybeNewParentId match {
        case None => Success(())
        case Some(id) if id == category.id =>
          Failure(
            ValidationException(
              "parentCategoryId",
              "Category cannot be its own child or the child of one of its children."
            )
          )
        case Some(_) if category.subcategories.nonEmpty =>
          category.subcategories
            .traverse(sub => validateParentCategory(sub, maybeNewParentId, user)(session))
            .map(_ => ())
        case Some(parentId) =>
          arenaRepository.getCategory(parentId, includeHidden = user.isAdmin)(session).flatMap {
            case Some(_) => Success(())
            case None =>
              Failure(
                ValidationException(
                  "parentCategoryId",
                  s"Could not find specified parent category id: '$parentId'"
                )
              )
          }
      }
    }

    def updateCategory(categoryId: Long, newCategory: NewCategoryDTO, user: MyNDLAUser)(
        session: DBSession = AutoSession
    ): Try[CategoryDTO] = {
      val toInsert = domain.InsertCategory(
        newCategory.title,
        newCategory.description,
        newCategory.visible,
        newCategory.parentCategoryId
      )
      for {
        existing      <- getCategory(categoryId, 0, 0, user)(session)
        _             <- validateParentCategory(existing, newCategory.parentCategoryId, user)(session)
        newRank       <- getNewRank(existing, toInsert.parentCategoryId)(session)
        updated       <- arenaRepository.updateCategory(categoryId, toInsert, newRank)(session)
        following     <- arenaRepository.getCategoryFollowing(categoryId, user.id)(session)
        subcategories <- getCategories(user, filterFollowed = false, CategorySortDTO.ByRank, categoryId.some)(session)
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

    def postTopic(categoryId: Long, newTopic: NewTopicDTO, user: MyNDLAUser): Try[api.TopicDTO] = {
      DBUtil.withSession { session =>
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
          compiledTopic = CompiledTopic(topic, Some(user), 1, isFollowing = true, 0)
        } yield converterService.toApiTopic(compiledTopic)
      }
    }

    private def failIfPostDisallowed(topic: CompiledTopic, user: MyNDLAUser): Try[Unit] = {
      if (user.isAdmin) return Success(())
      if (topic.topic.locked) return Failure(AccessDeniedException.forbidden)
      Success(())
    }

    def postPost(topicId: Long, newPost: NewPostDTO, user: MyNDLAUser): Try[api.PostDTO] =
      DBUtil.withSession { session =>
        val created = clock.now()
        for {
          topic <- getCompiledTopic(topicId, user)(session)
          _     <- failIfPostDisallowed(topic, user)
          newPost <- arenaRepository.postPost(topicId, newPost.content, user.id, created, created, newPost.toPostId)(
            session
          )
          _ <- generateNewPostNotifications(topic, newPost)(session)
          _ <- followTopic(topicId, user)(session)
          compiledPost = CompiledPost(newPost, Some(user), List.empty, 0, false)
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
    ): Try[api.CategoryWithTopicsDTO] = {
      val offset = (page - 1) * pageSize
      for {
        maybeCategory <- arenaRepository.getCategory(categoryId, includeHidden = requester.isAdmin)(session)
        category      <- maybeCategory.toTry(NotFoundException(s"Could not find category with id $categoryId"))
        topics        <- arenaRepository.getTopicsForCategory(categoryId, offset, pageSize, requester)(session)
        subcats <- getCategories(requester, filterFollowed = false, CategorySortDTO.ByRank, category.id.some)(session)
        topicsCount <- arenaRepository.getTopicCountForCategory(categoryId)(session)
        breadcrumb  <- arenaRepository.getBreadcrumbs(categoryId)(session)
        postsCount  <- arenaRepository.getPostCountForCategory(categoryId)(session)
        following   <- arenaRepository.getCategoryFollowing(categoryId, requester.id)(session)
        tt = topics.map(topic => converterService.toApiTopic(topic))
      } yield api.CategoryWithTopicsDTO(
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
    ): Try[api.TopicWithPostsDTO] = {
      val offset = (page - 1) * pageSize
      for {
        topic    <- getCompiledTopic(topicId, user)(session)
        posts    <- arenaRepository.getPostsForTopic(topicId, offset, pageSize, user)(session)
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
    ): Try[List[api.PostDTO]] = Try {
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

    def followTopic(topicId: Long, user: MyNDLAUser)(session: DBSession = AutoSession): Try[api.TopicWithPostsDTO] = {
      for {
        apiTopic  <- getTopic(topicId, user, 1, 10)(session)
        following <- arenaRepository.getTopicFollowing(topicId, user.id)(session)
        _         <- if (following.isEmpty) arenaRepository.followTopic(topicId, user.id)(session) else Success(())
      } yield apiTopic
    }

    def unfollowTopic(topicId: Long, user: MyNDLAUser)(session: DBSession = AutoSession): Try[api.TopicWithPostsDTO] = {
      for {
        apiTopic  <- getTopic(topicId, user, 0, 0)(session)
        following <- arenaRepository.getTopicFollowing(topicId, user.id)(session)
        _         <- if (following.isDefined) arenaRepository.unfollowTopic(topicId, user.id)(session) else Success(())
      } yield apiTopic
    }

    def upvotePost(postId: Long, user: MyNDLAUser)(session: DBSession = AutoSession): Try[api.PostDTO] = {
      for {
        maybePost     <- arenaRepository.getPost(postId)(session)
        (post, owner) <- maybePost.toTry(NotFoundException(s"Could not find post with id $postId"))
        upvoted       <- arenaRepository.getUpvoted(postId, user.id)(session)
        userIsNotOwner = owner.exists(_.id != user.id)
        _ <-
          if (upvoted.isDefined) {
            Failure(InvalidStateException(s"User ${user.id} has already upvoted"))
          } else if (upvoted.isEmpty && userIsNotOwner) arenaRepository.upvotePost(postId, user.id)(session)
          else Success(())
        flags   <- arenaRepository.getFlagsForPost(postId)(session)
        upvotes <- arenaRepository.getUpvotesForPost(postId)(session)
        compiledPost = CompiledPost(post, owner, flags, upvotes.length, userIsNotOwner)
        replies      = getRepliesForPost(compiledPost.post.id, user)(session).?
      } yield converterService.toApiPost(compiledPost, user, replies)
    }

    def unUpvotePost(postId: Long, user: MyNDLAUser)(session: DBSession = AutoSession): Try[api.PostDTO] = {
      for {
        maybePost     <- arenaRepository.getPost(postId)(session)
        (post, owner) <- maybePost.toTry(NotFoundException(s"Could not find post with id $postId"))
        upvoted       <- arenaRepository.getUpvoted(postId, user.id)(session)
        _             <- if (upvoted.isDefined) arenaRepository.unUpvotePost(postId, user.id)(session) else Success(())
        flags         <- arenaRepository.getFlagsForPost(postId)(session)
        upvotes       <- arenaRepository.getUpvotesForPost(postId)(session)
        compiledPost = CompiledPost(post, owner, flags, upvotes.length, upvoted = false)
        replies      = getRepliesForPost(compiledPost.post.id, user)(session).?
      } yield converterService.toApiPost(compiledPost, user, replies)
    }

    def followCategory(categoryId: Long, user: MyNDLAUser)(
        session: DBSession = AutoSession
    ): Try[api.CategoryWithTopicsDTO] = {
      for {
        apiCategory <- getCategory(categoryId, 1, 10, user)(session)
        following   <- arenaRepository.getCategoryFollowing(categoryId, user.id)(session)
        _ <- if (following.isEmpty) arenaRepository.followCategory(categoryId, user.id)(session) else Success(())
      } yield apiCategory
    }

    def unfollowCategory(categoryId: Long, user: MyNDLAUser)(
        session: DBSession = AutoSession
    ): Try[api.CategoryWithTopicsDTO] = {
      for {
        apiTopic  <- getCategory(categoryId, 1, 10, user)(session)
        following <- arenaRepository.getCategoryFollowing(categoryId, user.id)(session)
        _ <- if (following.isDefined) arenaRepository.unfollowCategory(categoryId, user.id)(session) else Success(())
      } yield apiTopic
    }

    def getCategories(
        requester: MyNDLAUser,
        filterFollowed: Boolean,
        sort: CategorySortDTO,
        parentCategoryId: Option[Long]
    )(session: DBSession = ReadOnlyAutoSession): Try[List[api.CategoryDTO]] =
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
      DBUtil.rollbackOnFailure(session => {
        for {
          feideToken   <- feideApiClient.getFeideAccessTokenOrFail(feideAccessToken)
          feideId      <- feideApiClient.getFeideID(feideAccessToken)
          user         <- userService.getOrCreateMyNDLAUserIfNotExist(feideId, feideAccessToken)(session)
          nodebbUserId <- nodebb.getUserId(feideToken)
          _            <- arenaRepository.disconnectPostsByUser(user.id)(session)
          _            <- arenaRepository.disconnectTopicsByUser(user.id)(session)
          _            <- arenaRepository.disconnectFlagsByUser(user.id)(session)
          _            <- folderRepository.deleteAllUserFolders(feideId)(session)
          _            <- folderRepository.deleteAllUserResources(feideId)(session)
          _            <- userRepository.deleteUser(feideId)(session)
          _            <- nodebb.deleteUser(nodebbUserId, feideToken)
        } yield ()
      })
  }
}
