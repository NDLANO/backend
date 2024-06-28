/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.model.arena.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.model.NDLADate
import no.ndla.myndlaapi.model.api.ArenaUser
import sttp.tapir.Schema.annotations.description

@description("Arena flag data")
case class Flag(
    @description("The flag id") id: Long,
    @description("The flag reason") reason: String,
    @description("The flag creation date") created: NDLADate,
    @description("The flag resolution date") resolved: Option[NDLADate],
    @description("Whether the flag has been resolved or not") isResolved: Boolean,
    @description("The flagging user") flagger: Option[ArenaUser]
)

object Flag {
  implicit val flagEncoder: Encoder[Flag] = deriveEncoder
  implicit val flagDecoder: Decoder[Flag] = deriveDecoder
}
