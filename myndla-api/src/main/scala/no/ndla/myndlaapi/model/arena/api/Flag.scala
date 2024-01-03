/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.model.arena.api

import no.ndla.common.model.NDLADate
import no.ndla.myndla.model.api.ArenaOwner
import sttp.tapir.Schema.annotations.description

@description("Arena flag data")
case class Flag(
    @description("The flag id") id: Long,
    @description("The flag reason") reason: String,
    @description("The flag creation date") created: NDLADate,
    @description("The flagging user") flagger: ArenaOwner
)
