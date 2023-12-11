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
import no.ndla.myndla.service.{ConfigService, UserService}
import no.ndla.network.clients.FeideApiClient
import no.ndla.myndlaapi.model.arena.api
import no.ndla.myndlaapi.model.arena.api.NewTopic
import no.ndla.myndlaapi.repository.ArenaRepository

import scala.util.{Failure, Success, Try}

trait ArenaReadService {
  this: FeideApiClient with ArenaRepository with ConverterService with UserService with Clock with ConfigService =>
  val arenaReadService: ArenaReadService

  class ArenaReadService {
    def postTopic(categoryId: Long, newTopic: NewTopic, feideHeader: Option[String]): Try[api.Topic] = {
      // TODO: arena enabled user?
      arenaRepository.withSession { session =>
        for {
          user <- userService.getMyNdlaUserDataDomain(feideHeader)
          created = clock.now()
          topic <- arenaRepository.postTopic(categoryId, newTopic.title, user.id, created)(session)
          post  <- arenaRepository.postPost(topic.id, newTopic.initialPost.content, user.id)(session)
        } yield converterService.toApiTopic(topic, List((post, user)))

      }
    }

    def getCategory(categoryId: Long): Try[api.CategoryWithTopics] = {
      arenaRepository.withSession { session =>
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
    }

    def getTopic(topicId: Long): Try[api.Topic] = {
      arenaRepository.withSession { session =>
        for {
          maybeTopic     <- arenaRepository.getTopic(topicId)(session)
          (topic, posts) <- maybeTopic.toTry(NotFoundException(s"Could not find topic with id $topicId"))
        } yield converterService.toApiTopic(topic, posts)
      }
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
