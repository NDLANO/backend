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
import no.ndla.myndlaapi.model.arena.api.{Category, NewCategory, NewPost, NewTopic}
import no.ndla.myndlaapi.model.arena.domain.MissingPostException
import no.ndla.myndlaapi.repository.ArenaRepository
import scalikejdbc.{AutoSession, DBSession, ReadOnlyAutoSession}

import scala.util.Try

trait ArenaReadService {
  this: FeideApiClient with ArenaRepository with ConverterService with UserService with Clock with ConfigService =>
  val arenaReadService: ArenaReadService

  class ArenaReadService {
    def updateTopic(topicId: Long, newTopic: NewTopic, user: MyNDLAUser)(
        session: DBSession = AutoSession
    ): Try[api.Topic] = {
      val updatedTime = clock.now()
      for {
        maybeTopic   <- arenaRepository.getTopic(topicId)(session)
        (_, posts)   <- maybeTopic.toTry(NotFoundException(s"Could not find topic with id $topicId"))
        updatedTopic <- arenaRepository.updateTopic(topicId, newTopic.title, updatedTime)(session)
        mainPostId   <- posts.headOption.map(_._1.id).toTry(MissingPostException("Could not find main post for topic"))
        updatedPost  <- arenaRepository.updatePost(mainPostId, newTopic.initialPost.content, updatedTime)(session)
      } yield converterService.toApiTopic(updatedTopic, (updatedPost, user) +: posts.tail)
    }

    def newCategory(newCategory: NewCategory, user: MyNDLAUser)(session: DBSession = AutoSession): Try[Category] = {
      val toInsert = domain.InsertCategory(newCategory.title, newCategory.description)
      arenaRepository.insertCategory(toInsert)(session).map { inserted =>
        converterService.toApiCategory(inserted, 0, 0)
      }
    }

    def updateCategory(categoryId: Long, newCategory: NewCategory, user: MyNDLAUser)(
        session: DBSession = AutoSession
    ): Try[Category] = {
      val toInsert = domain.InsertCategory(newCategory.title, newCategory.description)
      for {
        existing <- getCategory(categoryId)(session)
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

    def getCategory(categoryId: Long)(session: DBSession = ReadOnlyAutoSession): Try[api.CategoryWithTopics] = {
      for {
        maybeCategory <- arenaRepository.getCategory(categoryId)(session)
        category      <- maybeCategory.toTry(NotFoundException(s"Could not find category with id $categoryId"))
        topics        <- arenaRepository.getTopicsForCategory(categoryId)(session)
        topicsCount   <- arenaRepository.getTopicCountForCategory(categoryId)(session)
        postsCount    <- arenaRepository.getPostCountForCategory(categoryId)(session)
      } yield api.CategoryWithTopics(
        id = categoryId,
        title = category.title,
        description = category.description,
        topicCount = topicsCount,
        postCount = postsCount,
        topics = topics.map { case (topic, posts) => converterService.toApiTopic(topic, posts) }
      )
    }

    def getTopic(topicId: Long): Try[api.Topic] =
      arenaRepository.withSession { session =>
        for {
          maybeTopic     <- arenaRepository.getTopic(topicId)(session)
          (topic, posts) <- maybeTopic.toTry(NotFoundException(s"Could not find topic with id $topicId"))
        } yield converterService.toApiTopic(topic, posts)
      }

    def getCategories: Try[List[api.Category]] = {
      arenaRepository.withSession(session => {
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
      })
    }

  }
}
