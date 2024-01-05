/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.service

import cats.implicits._
import no.ndla.common.Clock
import no.ndla.common.errors.{AccessDeniedException, NotFoundException}
import no.ndla.common.implicits.OptionImplicit
import no.ndla.myndla.model.domain.MyNDLAUser
import no.ndla.myndla.service.{ConfigService, UserService}
import no.ndla.network.clients.FeideApiClient
import no.ndla.myndlaapi.model.arena.{api, domain}
import no.ndla.myndlaapi.model.arena.api.{Category, NewCategory, NewPost, NewTopic}
import no.ndla.myndlaapi.model.arena.domain.MissingPostException
import no.ndla.myndlaapi.model.arena.domain.database.{CompiledPost, CompiledTopic}
import no.ndla.myndlaapi.repository.ArenaRepository
import scalikejdbc.{AutoSession, DBSession, ReadOnlyAutoSession}

import scala.util.{Failure, Success, Try}

trait ArenaReadService {
  this: FeideApiClient with ArenaRepository with ConverterService with UserService with Clock with ConfigService =>
  val arenaReadService: ArenaReadService

  class ArenaReadService {
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
        apiPosts = posts.map(compiledPost => converterService.toApiPost(compiledPost, requester))
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
          api.NewPostNotification(
            id = notification.notification.id,
            isRead = notification.notification.is_read,
            topicTitle = notification.topic.title,
            topicId = notification.topic.id,
            post = converterService.toApiPost(notification.post, user),
            notificationTime = notification.notification.notification_time
          )
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
      maybeTopic <- arenaRepository.getCategory(categoryId)(session)
      _          <- maybeTopic.toTry(NotFoundException(s"Could not find category with id $categoryId"))
      _          <- arenaRepository.deleteCategory(categoryId)(session)
    } yield ()

    def deleteTopic(topicId: Long, user: MyNDLAUser)(session: DBSession = AutoSession): Try[Unit] = for {
      maybeTopic <- arenaRepository.getTopic(topicId, user)(session)
      topic      <- maybeTopic.toTry(NotFoundException(s"Could not find topic with id $topicId"))
      _          <- failIfEditDisallowed(topic.topic, user)
      _          <- arenaRepository.deleteTopic(topicId)(session)
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
        maybeCategory <- arenaRepository.getCategory(categoryId)(session)
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
        maybeTopic   <- arenaRepository.getTopic(topicId, user)(session)
        topic        <- maybeTopic.toTry(NotFoundException(s"Could not find topic with id $topicId"))
        posts        <- arenaRepository.getPostsForTopic(topicId, 0, 10)(session)
        _            <- failIfEditDisallowed(topic.topic, user)
        updatedTopic <- arenaRepository.updateTopic(topicId, newTopic.title, updatedTime)(session)
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
        compiledPost = CompiledPost(updatedPost, owner, flags)
      } yield converterService.toApiPost(compiledPost, user)
    }

    private def failIfEditDisallowed(owned: domain.Owned, user: MyNDLAUser): Try[Unit] =
      if (owned.ownerId == user.id || user.isAdmin) Success(())
      else Failure(AccessDeniedException.forbidden)

    def newCategory(newCategory: NewCategory)(session: DBSession = AutoSession): Try[Category] = {
      val toInsert = domain.InsertCategory(newCategory.title, newCategory.description)
      arenaRepository.insertCategory(toInsert)(session).map { inserted =>
        converterService.toApiCategory(inserted, 0, 0, isFollowing = false)
      }
    }

    def updateCategory(categoryId: Long, newCategory: NewCategory, user: MyNDLAUser)(
        session: DBSession = AutoSession
    ): Try[Category] = {
      val toInsert = domain.InsertCategory(newCategory.title, newCategory.description)
      for {
        existing  <- getCategory(categoryId, 0, 0, user)(session)
        updated   <- arenaRepository.updateCategory(categoryId, toInsert)(session)
        following <- arenaRepository.getCategoryFollowing(categoryId, user.id)(session)
      } yield converterService.toApiCategory(updated, existing.topicCount, existing.postCount, following.isDefined)
    }

    def postTopic(categoryId: Long, newTopic: NewTopic, user: MyNDLAUser): Try[api.Topic] = {
      arenaRepository.withSession { session =>
        val created = clock.now()
        for {
          _     <- getCategory(categoryId, 0, 0, user)(session)
          topic <- arenaRepository.insertTopic(categoryId, newTopic.title, user.id, created, created)(session)
          _     <- followTopic(topic.id, user)(session)
          _     <- arenaRepository.postPost(topic.id, newTopic.initialPost.content, user.id, created, created)(session)
          compiledTopic = CompiledTopic(topic, user, 1, isFollowing = true)
        } yield converterService.toApiTopic(compiledTopic)
      }
    }

    def postPost(topicId: Long, newPost: NewPost, user: MyNDLAUser): Try[api.Topic] =
      arenaRepository.withSession { session =>
        val created = clock.now()
        for {
          maybeBeforeTopic <- arenaRepository.getTopic(topicId, user)(session)
          topic            <- maybeBeforeTopic.toTry(NotFoundException(s"Could not find topic with id $topicId"))
          newPost          <- arenaRepository.postPost(topicId, newPost.content, user.id, created, created)(session)
          _                <- generateNewPostNotifications(topic, newPost)(session)
          _                <- followTopic(topicId, user)(session)
        } yield converterService.toApiTopic(topic)
      }

    def generateNewPostNotifications(topic: CompiledTopic, newPost: domain.Post)(
        session: DBSession
    ): Try[List[domain.Notification]] = {
      val notificationTime = clock.now()
      getFollowers(topic.topic.id)(session)
        .flatMap { followers =>
          followers.traverse { follower =>
            if (follower.id == newPost.ownerId) Success(None)
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
        maybeCategory <- arenaRepository.getCategory(categoryId)(session)
        category      <- maybeCategory.toTry(NotFoundException(s"Could not find category with id $categoryId"))
        topics        <- arenaRepository.getTopicsForCategory(categoryId, offset, pageSize, requester)(session)
        topicsCount   <- arenaRepository.getTopicCountForCategory(categoryId)(session)
        postsCount    <- arenaRepository.getPostCountForCategory(categoryId)(session)
        following     <- arenaRepository.getCategoryFollowing(categoryId, requester.id)(session)
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
        isFollowing = following.isDefined
      )
    }

    def getTopic(topicId: Long, user: MyNDLAUser, page: Long, pageSize: Long)(
        session: DBSession = ReadOnlyAutoSession
    ): Try[api.TopicWithPosts] = {
      val offset = (page - 1) * pageSize
      for {
        maybeTopic <- arenaRepository.getTopic(topicId, user)(session)
        topic      <- maybeTopic.toTry(NotFoundException(s"Could not find topic with id $topicId"))
        posts      <- arenaRepository.getPostsForTopic(topicId, offset, pageSize)(session)
      } yield converterService.toApiTopicWithPosts(
        compiledTopic = topic,
        page = page,
        pageSize = pageSize,
        posts = posts,
        requester = user
      )
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

    def getCategories(requester: MyNDLAUser, filterFollowed: Boolean)(
        session: DBSession = ReadOnlyAutoSession
    ): Try[List[api.Category]] =
      arenaRepository
        .getCategories(requester, filterFollowed)(session)
        .flatMap(categories => {
          categories.traverse(category => {
            for {
              postCount  <- arenaRepository.getPostCountForCategory(category.id)(session)
              topicCount <- arenaRepository.getTopicCountForCategory(category.id)(session)
              following  <- arenaRepository.getCategoryFollowing(category.id, requester.id)(session)
            } yield converterService.toApiCategory(category, topicCount, postCount, following.isDefined)
          })
        })

  }
}
