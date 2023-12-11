/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.model.arena.api

import sttp.tapir.Schema.annotations.description

@description("Arena owner data")
case class Owner(
    @description("The owners id") id: Long,
    @description("The name") name: String
)
