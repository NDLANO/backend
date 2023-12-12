/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.model.arena.api

import sttp.tapir.Schema.annotations.description

@description("Arena category data")
case class NewCategory(
    @description("The category's title") title: String,
    @description("The category's description") description: String
)
