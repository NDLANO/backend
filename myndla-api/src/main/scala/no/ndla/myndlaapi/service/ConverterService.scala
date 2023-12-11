/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.service

import no.ndla.myndlaapi.model.arena.{api, domain}

trait ConverterService {
  val converterService: ConverterService

  class ConverterService {

    def toApiCategory(category: domain.Category, topicCount: Long, postCount: Long): api.Category = {
      api.Category(
        id = category.id,
        title = category.title,
        description = category.description,
        topicCount = topicCount,
        postCount = postCount
      )
    }

    def toApiTopic(topic: domain.Topic, posts: List[domain.Post]): api.Topic = {
      val apiPosts = posts.map(post => toApiPost(post))
      api.Topic(
        id = topic.id,
        title = topic.title,
        content = topic.content,
        created = topic.created,
        updated = topic.updated,
        posts = apiPosts,
        postCount = apiPosts.size
      )
    }

    def toApiPost(post: domain.Post): api.Post = {
      api.Post(
        id = post.id,
        content = post.content,
        created = post.created,
        updated = post.updated
      )
    }

  }
}
