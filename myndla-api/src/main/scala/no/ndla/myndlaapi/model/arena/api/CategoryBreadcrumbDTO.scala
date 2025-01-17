/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.model.arena.api

import sttp.tapir.Schema.annotations.description

@description("Arena category data")
case class CategoryBreadcrumbDTO(
    @description("The category's id") id: Long,
    @description("The category's title") title: String
)
