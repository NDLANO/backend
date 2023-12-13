/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.model.arena.api

import no.ndla.common.model.NDLADate
import sttp.tapir.Schema.annotations.description

@description("Arena post data")
case class Post(
    @description("The post id") id: Long,
    @description("The post content") content: String,
    @description("The post creation date") created: NDLADate,
    @description("The post edit date") updated: NDLADate,
    @description("The post owner") owner: Owner,
    @description("The flags that have been added to post. Only visible to admins.") flags: Option[List[Flag]]
)
