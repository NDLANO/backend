/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import sttp.tapir.Schema.annotations.description
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.model.NDLADate

@description("Meta information about podcast audio")
case class NewPodcastMetaDTO(
    @description("Introduction for the podcast")
    introduction: String,
    @description("Cover photo for the podcast")
    coverPhotoId: String,
    @description("Cover photo alttext for the podcast")
    coverPhotoAltText: String,
    @description("The time the podcast was released from its source")
    released: Option[NDLADate],
)
object NewPodcastMetaDTO {
  implicit val encoder: Encoder[NewPodcastMetaDTO] = deriveEncoder
  implicit val decoder: Decoder[NewPodcastMetaDTO] = deriveDecoder
}
