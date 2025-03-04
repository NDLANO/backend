/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import sttp.tapir.Schema.annotations.description

@description("Administrator message left on learningpaths")
case class MessageDTO(
    @description("Message left on a learningpath by administrator") message: String,
    @description("When the message was left") date: NDLADate
)

object MessageDTO {
  implicit val encoder: Encoder[MessageDTO] = deriveEncoder
  implicit val decoder: Decoder[MessageDTO] = deriveDecoder
}
