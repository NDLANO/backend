/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.service

import no.ndla.myndla.model.domain.MyNDLAUser
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

    def toApiTopic(topic: domain.Topic, posts: List[(domain.Post, MyNDLAUser)]): api.Topic = {
      val apiPosts = posts.map{case (post, owner) => toApiPost(post, owner)}
      api.Topic(
        id = topic.id,
        title = topic.title,
        created = topic.created,
        updated = topic.updated,
        posts = apiPosts,
        postCount = apiPosts.size.toLong
      )
    }

    def toApiPost(post: domain.Post, owner: MyNDLAUser): api.Post = {
      api.Post(
        id = post.id,
        content = post.content,
        created = post.created,
        updated = post.updated,
        owner = api.Owner(
          id = owner.id,
          name = owner.displayName
        )
      )
    }

  }
}
