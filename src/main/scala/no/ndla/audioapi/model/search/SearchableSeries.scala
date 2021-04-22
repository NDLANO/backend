/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.search

import no.ndla.audioapi.model.domain.CoverPhoto

case class SearchableSeries(
    id: String,
    titles: SearchableLanguageValues,
    episodes: Seq[SearchableAudioInformation],
    coverPhoto: CoverPhoto
)
