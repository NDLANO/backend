/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.search

import no.ndla.audioapi.model.domain.CoverPhoto
import no.ndla.common.model.NDLADate
import no.ndla.search.model.SearchableLanguageValues

case class SearchableSeries(
    id: String,
    titles: SearchableLanguageValues,
    descriptions: SearchableLanguageValues,
    episodes: Option[Seq[SearchableAudioInformation]],
    coverPhoto: CoverPhoto,
    lastUpdated: NDLADate
)
