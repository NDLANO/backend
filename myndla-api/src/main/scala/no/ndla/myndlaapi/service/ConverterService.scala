/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.service

import no.ndla.myndla.model.domain.MyNDLAUser
import no.ndla.myndlaapi.model.arena.domain.database.{CompiledFlag, CompiledPost, CompiledTopic}
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

    def toApiTopic(compiledTopic: CompiledTopic, postCount: Long): api.Topic = {
      api.Topic(
        id = compiledTopic.topic.id,
        title = compiledTopic.topic.title,
        created = compiledTopic.topic.created,
        updated = compiledTopic.topic.updated,
        postCount = postCount
      )
    }

    def toApiTopicWithPosts(
        compiledTopic: CompiledTopic,
        page: Long,
        pageSize: Long,
        postCount: Long,
        posts: List[CompiledPost],
        requester: MyNDLAUser
    ): api.TopicWithPosts = {
      val apiPosts = posts.map(post => toApiPost(post, requester))
      val pagination = api.Paginated(
        page = page,
        pageSize = pageSize,
        totalCount = postCount,
        items = apiPosts
      )

      api.TopicWithPosts(
        id = compiledTopic.topic.id,
        title = compiledTopic.topic.title,
        created = compiledTopic.topic.created,
        updated = compiledTopic.topic.updated,
        postCount = apiPosts.size.toLong,
        posts = pagination
      )
    }

    def toOwner(user: MyNDLAUser): api.Owner = {
      api.Owner(
        id = user.id,
        name = user.displayName
      )
    }

    def toApiFlag(flag: CompiledFlag): api.Flag = {
      api.Flag(
        id = flag.flag.id,
        reason = flag.flag.reason,
        created = flag.flag.created,
        flagger = toOwner(flag.flagger)
      )
    }

    def toApiPost(
        compiledPost: CompiledPost,
        requester: MyNDLAUser
    ): api.Post = {
      val maybeFlags = Option.when(requester.arenaAdmin.contains(true))(compiledPost.flags.map(toApiFlag))
      api.Post(
        id = compiledPost.post.id,
        content = compiledPost.post.content,
        created = compiledPost.post.created,
        updated = compiledPost.post.updated,
        owner = toOwner(compiledPost.owner),
        flags = maybeFlags
      )
    }

  }
}
