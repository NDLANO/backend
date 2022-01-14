/*
 * Part of NDLA audio-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.domain

import no.ndla.language.model.WithLanguage

/** Metadata fields for [[AudioType.Podcast]] type audios */
case class PodcastMeta(
    introduction: String,
    coverPhoto: CoverPhoto,
    language: String
) extends WithLanguage
