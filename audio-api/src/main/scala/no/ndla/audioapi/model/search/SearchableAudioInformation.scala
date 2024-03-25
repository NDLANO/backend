/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.audioapi.model.domain.CoverPhoto
import no.ndla.common.model.NDLADate
import no.ndla.search.model.{SearchableLanguageList, SearchableLanguageValues}

case class SearchablePodcastMeta(
    coverPhoto: CoverPhoto,
    language: String
)

object SearchablePodcastMeta {
  implicit val encoder: Encoder[SearchablePodcastMeta] = deriveEncoder
  implicit val decoder: Decoder[SearchablePodcastMeta] = deriveDecoder
}

// Only used to calculate supportedLanguages
case class SearchableAudio(
    filePath: String,
    language: String
)

object SearchableAudio {
  implicit val encoder: Encoder[SearchableAudio] = deriveEncoder
  implicit val decoder: Decoder[SearchableAudio] = deriveDecoder
}

case class SearchableAudioInformation(
    id: String,
    titles: SearchableLanguageValues,
    tags: SearchableLanguageList,
    filePaths: Seq[SearchableAudio],
    license: String,
    authors: Seq[String],
    lastUpdated: NDLADate,
    defaultTitle: Option[String],
    audioType: String,
    podcastMetaIntroduction: SearchableLanguageValues,
    podcastMeta: Seq[SearchablePodcastMeta],
    manuscript: SearchableLanguageValues,
    series: Option[SearchableSeries]
)

object SearchableAudioInformation {
  implicit val encoder: Encoder[SearchableAudioInformation] = deriveEncoder
  implicit val decoder: Decoder[SearchableAudioInformation] = deriveDecoder
}
