/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.service

import cats.implicits._
import no.ndla.common.Clock
import no.ndla.common.errors.NotFoundException
import no.ndla.common.implicits.OptionImplicit
import no.ndla.myndla.model.domain.MyNDLAUser
import no.ndla.myndla.service.{ConfigService, UserService}
import no.ndla.network.clients.FeideApiClient
import no.ndla.myndlaapi.model.arena.{api, domain}
import no.ndla.myndlaapi.model.arena.api.{Category, NewCategory, NewPost, NewTopic, Paginated}
import no.ndla.myndlaapi.model.arena.domain.MissingPostException
import no.ndla.myndlaapi.repository.ArenaRepository
import scalikejdbc.{AutoSession, DBSession, ReadOnlyAutoSession}

import scala.util.{Failure, Success, Try}

trait ArenaReadService {
  this: FeideApiClient with ArenaRepository with ConverterService with UserService with Clock with ConfigService =>
  val arenaReadService: ArenaReadService

  class ArenaReadService {
    def deletePost(postId: Long, user: MyNDLAUser)(session: DBSession= AutoSession): Try[Unit] = {
        for {
            maybePost <- arenaRepository.getPost(postId)(session)
            post      <- maybePost.toTry(NotFoundException(s"Could not find post with id $postId"))
            _         <- failIfEditDisallowed(post, user)
            _         <- arenaRepository.deletePost(postId)(session)
        } yield ()

    }

    def getTopicsForCategory(categoryId: Long, page: Long, pageSize: Long)(
        session: DBSession = ReadOnlyAutoSession
    ): Try[Paginated[api.Topic]] = {
      val offset = (page - 1) * pageSize
      for {
        maybeCategory <- arenaRepository.getCategory(categoryId)(session)
        _             <- maybeCategory.toTry(NotFoundException(s"Could not find category with id $categoryId"))
        topics        <- arenaRepository.getTopicsForCategory(categoryId, offset, pageSize)(session)
        topicsCount   <- arenaRepository.getTopicCountForCategory(categoryId)(session)
      } yield Paginated[api.Topic](
        items = topics.map { case (topic, posts) => converterService.toApiTopic(topic, posts) },
        totalCount = topicsCount,
        pageSize = pageSize,
        page = page
      )
    }

    def getRecentTopics(page: Long, pageSize: Long)(
        session: DBSession = ReadOnlyAutoSession
    ): Try[Paginated[api.Topic]] = {
      val offset = (page - 1) * pageSize
      for {
        topics      <- arenaRepository.getTopicsPaginated(offset, pageSize)(session)
        topicsCount <- arenaRepository.topicCount(session)
        apiTopics = topics.map { case (topic, posts) => converterService.toApiTopic(topic, posts) }
      } yield Paginated[api.Topic](
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
        maybeTopic     <- arenaRepository.getTopic(topicId)(session)
        (topic, posts) <- maybeTopic.toTry(NotFoundException(s"Could not find topic with id $topicId"))
        _              <- failIfEditDisallowed(topic, user)
        updatedTopic   <- arenaRepository.updateTopic(topicId, newTopic.title, updatedTime)(session)
        mainPostId  <- posts.headOption.map(_._1.id).toTry(MissingPostException("Could not find main post for topic"))
        updatedPost <- arenaRepository.updatePost(mainPostId, newTopic.initialPost.content, updatedTime)(session)
      } yield converterService.toApiTopic(updatedTopic, (updatedPost, user) +: posts.tail)
    }

    def updatePost(postId: Long, newPost: NewPost, user: MyNDLAUser)(
        session: DBSession = AutoSession
    ): Try[api.Post] = {
      val updatedTime = clock.now()
      for {
        maybePost   <- arenaRepository.getPost(postId)(session)
        post        <- maybePost.toTry(NotFoundException(s"Could not find post with id $postId"))
        _           <- failIfEditDisallowed(post, user)
        updatedPost <- arenaRepository.updatePost(postId, newPost.content, updatedTime)(session)
      } yield converterService.toApiPost(updatedPost, user)
    }

    private def failIfEditDisallowed(owned: domain.Owned, user: MyNDLAUser): Try[Unit] =
      if (owned.ownerId == user.id || user.arenaAdmin.contains(true)) Success(())
      else Failure(NotFoundException("Could not find topic with id"))

    def newCategory(newCategory: NewCategory)(session: DBSession = AutoSession): Try[Category] = {
      val toInsert = domain.InsertCategory(newCategory.title, newCategory.description)
      arenaRepository.insertCategory(toInsert)(session).map { inserted =>
        converterService.toApiCategory(inserted, 0, 0)
      }
    }

    def updateCategory(categoryId: Long, newCategory: NewCategory)(
        session: DBSession = AutoSession
    ): Try[Category] = {
      val toInsert = domain.InsertCategory(newCategory.title, newCategory.description)
      for {
        existing <- getCategory(categoryId, 0, 0)(session)
        updated  <- arenaRepository.updateCategory(categoryId, toInsert)(session)
      } yield converterService.toApiCategory(updated, existing.topicCount, existing.postCount)
    }

    def postTopic(categoryId: Long, newTopic: NewTopic, user: MyNDLAUser): Try[api.Topic] = {
      arenaRepository.withSession { session =>
        val created = clock.now()
        for {
          topic <- arenaRepository.insertTopic(categoryId, newTopic.title, user.id, created)(session)
          post  <- arenaRepository.postPost(topic.id, newTopic.initialPost.content, user.id)(session)
        } yield converterService.toApiTopic(topic, List((post, user)))
      }
    }

    def postPost(topicId: Long, newPost: NewPost, user: MyNDLAUser): Try[api.Topic] =
      arenaRepository.withSession { session =>
        for {
          _              <- arenaRepository.postPost(topicId, newPost.content, user.id)(session)
          maybeTopic     <- arenaRepository.getTopic(topicId)(session)
          (topic, posts) <- maybeTopic.toTry(NotFoundException(s"Could not find topic with id $topicId"))
        } yield converterService.toApiTopic(topic, posts)
      }

    def getCategory(categoryId: Long, page: Long, pageSize: Long)(
        session: DBSession = ReadOnlyAutoSession
    ): Try[api.CategoryWithTopics] = {
      val offset = (page - 1) * pageSize
      for {
        maybeCategory <- arenaRepository.getCategory(categoryId)(session)
        category      <- maybeCategory.toTry(NotFoundException(s"Could not find category with id $categoryId"))
        topics        <- arenaRepository.getTopicsForCategory(categoryId, offset, pageSize)(session)
        topicsCount   <- arenaRepository.getTopicCountForCategory(categoryId)(session)
        postsCount    <- arenaRepository.getPostCountForCategory(categoryId)(session)
      } yield api.CategoryWithTopics(
        id = categoryId,
        title = category.title,
        description = category.description,
        topicCount = topicsCount,
        postCount = postsCount,
        topics = topics.map { case (topic, posts) => converterService.toApiTopic(topic, posts) },
        topicPageSize = pageSize,
        topicPage = page
      )
    }

    def getTopic(topicId: Long): Try[api.Topic] =
      arenaRepository.withSession { session =>
        for {
          maybeTopic     <- arenaRepository.getTopic(topicId)(session)
          (topic, posts) <- maybeTopic.toTry(NotFoundException(s"Could not find topic with id $topicId"))
        } yield converterService.toApiTopic(topic, posts)
      }

    def getCategories(session: DBSession = ReadOnlyAutoSession): Try[List[api.Category]] =
      arenaRepository
        .getCategories(session)
        .flatMap(categories => {
          categories.traverse(category => {
            for {
              postCount  <- arenaRepository.getPostCountForCategory(category.id)(session)
              topicCount <- arenaRepository.getTopicCountForCategory(category.id)(session)
            } yield converterService.toApiCategory(category, topicCount, postCount)
          })
        })

  }
}
