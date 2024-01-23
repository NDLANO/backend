/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.model.arena.api

import sttp.tapir.Schema.annotations.description

@description("Arena topic data")
case class NewTopic(
    @description("The topics title") title: String,
    @description("The initial post in the topic") initialPost: NewPost
)
