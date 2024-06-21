/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.model.arena.domain.database

import no.ndla.myndlaapi.model.arena.domain
import no.ndla.myndlaapi.model.domain.MyNDLAUser

case class CompiledFlag(
    flag: domain.Flag,
    flagger: Option[MyNDLAUser]
)

case class CompiledPost(
    post: domain.Post,
    owner: Option[MyNDLAUser],
    flags: List[CompiledFlag],
    upvotes: Int,
    upvoted: Boolean
)

case class CompiledTopic(
    topic: domain.Topic,
    owner: Option[MyNDLAUser],
    postCount: Long,
    isFollowing: Boolean
)

case class CompiledNotification(
    notification: domain.Notification,
    post: CompiledPost,
    topic: domain.Topic,
    notifiedUser: MyNDLAUser
)
