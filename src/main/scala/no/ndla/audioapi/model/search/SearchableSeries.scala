/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.search

import no.ndla.audioapi.model.domain.CoverPhoto
import org.joda.time.DateTime

case class SearchableSeries(
    id: String,
    titles: SearchableLanguageValues,
    descriptions: SearchableLanguageValues,
    episodes: Option[Seq[SearchableAudioInformation]],
    coverPhoto: CoverPhoto,
    lastUpdated: DateTime,
)
