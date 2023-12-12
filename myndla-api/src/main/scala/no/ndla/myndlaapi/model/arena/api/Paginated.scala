/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.model.arena.api

import sttp.tapir.Schema.annotations.description

@description("Generic model to describe pagination of T")
case class Paginated[T](
    @description("How many items across all pages") totalCount: Long,
    @description("Which page number this is") page: Long,
    @description("How many items pr page") pageSize: Long,
    @description("The paginated items") items: List[T]
)
