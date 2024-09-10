/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.service

import no.ndla.myndlaapi.model.api.ArenaUser
import no.ndla.myndlaapi.model.arena.domain.database.{CompiledFlag, CompiledPost, CompiledTopic}
import no.ndla.myndlaapi.model.arena.{api, domain}
import no.ndla.myndlaapi.model.domain.MyNDLAUser

trait ConverterService {
  val converterService: ConverterService

  class ConverterService {

    def toApiCategory(
        category: domain.Category,
        topicCount: Long,
        postCount: Long,
        isFollowing: Boolean,
        subcategories: List[api.Category],
        breadcrumbs: List[api.CategoryBreadcrumb]
    ): api.Category = {
      api.Category(
        id = category.id,
        title = category.title,
        description = category.description,
        topicCount = topicCount,
        postCount = postCount,
        isFollowing = isFollowing,
        visible = category.visible,
        rank = category.rank,
        parentCategoryId = category.parentCategoryId,
        subcategories = subcategories,
        categoryCount = subcategories.size.toLong,
        breadcrumbs = breadcrumbs
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
        isPinned = compiledTopic.topic.pinned,
        voteCount = compiledTopic.voteCount
      )
    }

    def toApiTopicWithPosts(
        compiledTopic: CompiledTopic,
        page: Long,
        pageSize: Long,
        posts: List[api.Post]
    ): api.TopicWithPosts = {

      val pagination = api.PaginatedPosts(
        page = page,
        pageSize = pageSize,
        totalCount = compiledTopic.postCount,
        items = posts
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
        isPinned = compiledTopic.topic.pinned,
        voteCount = compiledTopic.voteCount
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
        requester: MyNDLAUser,
        replies: List[api.Post]
    ): api.Post = {
      val maybeFlags = Option.when(requester.isAdmin)(compiledPost.flags.map(toApiFlag))

      api.Post(
        id = compiledPost.post.id,
        content = compiledPost.post.content,
        created = compiledPost.post.created,
        updated = compiledPost.post.updated,
        owner = compiledPost.owner.map(ArenaUser.from),
        flags = maybeFlags,
        topicId = compiledPost.post.topic_id,
        replies = replies,
        upvotes = compiledPost.upvotes,
        upvoted = compiledPost.upvoted
      )
    }

  }
}
