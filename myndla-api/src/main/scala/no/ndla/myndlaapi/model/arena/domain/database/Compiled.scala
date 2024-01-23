/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.model.arena.domain.database

import no.ndla.myndla.model.domain.MyNDLAUser
import no.ndla.myndlaapi.model.arena.domain

case class CompiledFlag(
    flag: domain.Flag,
    flagger: MyNDLAUser
)

case class CompiledPost(
    post: domain.Post,
    owner: MyNDLAUser,
    flags: List[CompiledFlag]
)

case class CompiledTopic(
    topic: domain.Topic,
    owner: MyNDLAUser,
    postCount: Long,
    isFollowing: Boolean
)

case class CompiledNotification(
    notification: domain.Notification,
    post: CompiledPost,
    topic: domain.Topic,
    notifiedUser: MyNDLAUser
)
