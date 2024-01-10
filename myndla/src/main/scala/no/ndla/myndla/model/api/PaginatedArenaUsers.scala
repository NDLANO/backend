/*
 * Part of NDLA myndla.
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndla.model.api

import sttp.tapir.Schema.annotations.description

@description("Model to describe pagination of users")
case class PaginatedArenaUsers(
    @description("How many items across all pages") totalCount: Long,
    @description("Which page number this is") page: Long,
    @description("How many items pr page") pageSize: Long,
    @description("The paginated items") items: List[ArenaUser]
)
