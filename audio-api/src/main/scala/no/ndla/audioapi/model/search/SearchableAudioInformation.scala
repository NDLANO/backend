/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.search

import no.ndla.audioapi.model.domain.CoverPhoto
import no.ndla.search.model.{SearchableLanguageList, SearchableLanguageValues}

import java.util.Date

case class SearchablePodcastMeta(
    coverPhoto: CoverPhoto,
    language: String
)

// Only used to calculate supportedLanguages
case class SearchableAudio(
    filePath: String,
    language: String
)

case class SearchableAudioInformation(
    id: String,
    titles: SearchableLanguageValues,
    tags: SearchableLanguageList,
    filePaths: Seq[SearchableAudio],
    license: String,
    authors: Seq[String],
    lastUpdated: Date,
    defaultTitle: Option[String],
    audioType: String,
    podcastMetaIntroduction: SearchableLanguageValues,
    podcastMeta: Seq[SearchablePodcastMeta],
    manuscript: SearchableLanguageValues,
    series: Option[SearchableSeries]
)
