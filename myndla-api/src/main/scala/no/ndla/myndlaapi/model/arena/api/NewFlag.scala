/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.model.arena.api

import sttp.tapir.Schema.annotations.description

@description("Flag data")
case class NewFlag(
    @description("The reason for flagging post") reason: String
)
