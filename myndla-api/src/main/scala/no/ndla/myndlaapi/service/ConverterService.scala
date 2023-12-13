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

    def toApiTopic(
        topic: domain.Topic,
        posts: List[(domain.Post, MyNDLAUser, List[(domain.Flag, MyNDLAUser)])],
        requester: MyNDLAUser
    ): api.Topic = {
      val apiPosts = posts.map { case (post, owner, flags) => toApiPost(post, flags, owner, requester) }
      api.Topic(
        id = topic.id,
        title = topic.title,
        created = topic.created,
        updated = topic.updated,
        posts = apiPosts,
        postCount = apiPosts.size.toLong
      )
    }

    def toOwner(user: MyNDLAUser): api.Owner = {
      api.Owner(
        id = user.id,
        name = user.displayName
      )
    }

    def toApiFlag(flag: domain.Flag, flagger: MyNDLAUser): api.Flag = {
      api.Flag(
        id = flag.id,
        reason = flag.reason,
        created = flag.created,
        flagger = toOwner(flagger)
      )
    }

    def toApiPost(
        post: domain.Post,
        flags: List[(domain.Flag, MyNDLAUser)],
        owner: MyNDLAUser,
        requester: MyNDLAUser
    ): api.Post = {
      val maybeFlags = {
        if (requester.arenaAdmin.contains(true)) {
          Some(flags.map { case (flag, flagger) => toApiFlag(flag, flagger) })
        } else None
      }
      api.Post(
        id = post.id,
        content = post.content,
        created = post.created,
        updated = post.updated,
        owner = toOwner(owner),
        flags = maybeFlags
      )
    }

  }
}
