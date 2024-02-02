/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.service

import no.ndla.myndla.model.api.ArenaUser
import no.ndla.myndla.model.domain.MyNDLAUser
import no.ndla.myndlaapi.model.arena.domain.database.{CompiledFlag, CompiledPost, CompiledTopic}
import no.ndla.myndlaapi.model.arena.{api, domain}

trait ConverterService {
  val converterService: ConverterService

  class ConverterService {

    def toApiCategory(
        category: domain.Category,
        topicCount: Long,
        postCount: Long,
        isFollowing: Boolean
    ): api.Category = {
      api.Category(
        id = category.id,
        title = category.title,
        description = category.description,
        topicCount = topicCount,
        postCount = postCount,
        isFollowing = isFollowing,
        visible = category.visible,
        rank = category.rank
      )
    }

    def toApiTopic(compiledTopic: CompiledTopic): api.Topic = {
      api.Topic(
        id = compiledTopic.topic.id,
        title = compiledTopic.topic.title,
        created = compiledTopic.topic.created,
        updated = compiledTopic.topic.updated,
        postCount = compiledTopic.postCount,
        categoryId = compiledTopic.topic.category_id,
        isFollowing = compiledTopic.isFollowing,
        isLocked = compiledTopic.topic.locked,
        isPinned = compiledTopic.topic.pinned
      )
    }

    def toApiTopicWithPosts(
        compiledTopic: CompiledTopic,
        page: Long,
        pageSize: Long,
        posts: List[CompiledPost],
        requester: MyNDLAUser
    ): api.TopicWithPosts = {
      val apiPosts = posts.map(post => toApiPost(post, requester))
      val pagination = api.PaginatedPosts(
        page = page,
        pageSize = pageSize,
        totalCount = compiledTopic.postCount,
        items = apiPosts
      )

      api.TopicWithPosts(
        id = compiledTopic.topic.id,
        title = compiledTopic.topic.title,
        created = compiledTopic.topic.created,
        updated = compiledTopic.topic.updated,
        postCount = compiledTopic.postCount,
        posts = pagination,
        categoryId = compiledTopic.topic.category_id,
        isFollowing = compiledTopic.isFollowing,
        isLocked = compiledTopic.topic.locked,
        isPinned = compiledTopic.topic.pinned
      )
    }

    def toApiFlag(flag: CompiledFlag): api.Flag = {
      api.Flag(
        id = flag.flag.id,
        reason = flag.flag.reason,
        created = flag.flag.created,
        flagger = flag.flagger.map(ArenaUser.from),
        resolved = flag.flag.resolved,
        isResolved = flag.flag.resolved.isDefined
      )
    }

    def toApiPost(
        compiledPost: CompiledPost,
        requester: MyNDLAUser
    ): api.Post = {
      val maybeFlags = Option.when(requester.isAdmin)(compiledPost.flags.map(toApiFlag))
      api.Post(
        id = compiledPost.post.id,
        content = compiledPost.post.content,
        created = compiledPost.post.created,
        updated = compiledPost.post.updated,
        owner = compiledPost.owner.map(ArenaUser.from),
        flags = maybeFlags,
        topicId = compiledPost.post.topic_id
      )
    }

  }
}
