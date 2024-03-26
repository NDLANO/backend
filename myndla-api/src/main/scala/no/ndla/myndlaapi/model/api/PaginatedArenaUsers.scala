/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.api

import sttp.tapir.Schema.annotations.description

@description("Model to describe pagination of users")
case class PaginatedArenaUsers(
    @description("How many items across all pages") totalCount: Long,
    @description("Which page number this is") page: Long,
    @description("How many items per page") pageSize: Long,
    @description("The paginated items") items: List[ArenaUser]
)
