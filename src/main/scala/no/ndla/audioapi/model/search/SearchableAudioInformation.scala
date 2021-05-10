/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.search

import no.ndla.audioapi.model.domain.PodcastMeta

import java.util.Date

case class SearchableAudioInformation(
    id: String,
    titles: SearchableLanguageValues,
    tags: SearchableLanguageList,
    license: String,
    authors: Seq[String],
    lastUpdated: Date,
    defaultTitle: Option[String],
    audioType: String,
    podcastMeta: Seq[PodcastMeta],
)
