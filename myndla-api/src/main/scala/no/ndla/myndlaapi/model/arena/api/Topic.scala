/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.model.arena.api

import no.ndla.common.model.NDLADate
import sttp.tapir.Schema.annotations.description

@description("Arena topic data")
case class Topic(
    @description("The topics id") id: Long,
    @description("The topics title") title: String,
    @description("Count of posts in the topic") postCount: Long,
    @description("The post creation date") created: NDLADate,
    @description("The post edit date") updated: NDLADate,
    @description("The id of the parenting category") categoryId: Long,
    @description("Whether the requesting user is following the topic") isFollowing: Boolean,
    @description("Whether the topic is locked or not") isLocked: Boolean,
    @description("Whether the topic is pinned to the top of the category") isPinned: Boolean
)
