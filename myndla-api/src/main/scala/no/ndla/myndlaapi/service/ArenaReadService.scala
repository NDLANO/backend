/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.service

import cats.implicits._
import no.ndla.network.clients.FeideApiClient
import no.ndla.myndlaapi.model.arena.api
import no.ndla.myndlaapi.repository.ArenaRepository

import scala.util.{Success, Try}

trait ArenaReadService {
  this: FeideApiClient with ArenaRepository with ConverterService =>
  val arenaReadService: ArenaReadService

  class ArenaReadService {
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
