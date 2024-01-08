/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.model.arena.api

import sttp.tapir.Schema.annotations.description

@description("Arena category data")
case class Category(
    @description("The category's id") id: Long,
    @description("The category's title") title: String,
    @description("The category's description") description: String,
    @description("Count of topics in the category") topicCount: Long,
    @description("Count of posts in the category") postCount: Long,
    @description("Whether the requesting user is following the category") isFollowing: Boolean,
    @description("Whether the category is visible to regular users") visible: Boolean,
    @description("Where the category is sorted when sorting by rank") rank: Int
)
