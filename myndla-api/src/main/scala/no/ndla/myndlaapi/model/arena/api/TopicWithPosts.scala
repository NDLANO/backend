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
case class TopicWithPosts(
    @description("The topics id") id: Long,
    @description("The topics title") title: String,
    @description("Count of posts in the topic") postCount: Long,
    @description("The posts in the topic") posts: Paginated[Post],
    @description("The post creation date") created: NDLADate,
    @description("The post edit date") updated: NDLADate
)
