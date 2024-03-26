/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.domain

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.language.model.WithLanguage

/** Metadata fields for [[AudioType.Podcast]] type audios */
case class PodcastMeta(
    introduction: String,
    coverPhoto: CoverPhoto,
    language: String
) extends WithLanguage

object PodcastMeta {
  implicit val encoder: Encoder[PodcastMeta] = deriveEncoder
  implicit val decoder: Decoder[PodcastMeta] = deriveDecoder
}
