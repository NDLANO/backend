/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.search

import no.ndla.search.model.{SearchableLanguageList, SearchableLanguageValues}

import java.util.Date

case class SearchableImage(
    id: Long,
    titles: SearchableLanguageValues,
    alttexts: SearchableLanguageValues,
    captions: SearchableLanguageValues,
    tags: SearchableLanguageList,
    contributors: Seq[String],
    license: String,
    imageSize: Long,
    previewUrl: String,
    lastUpdated: Date,
    defaultTitle: Option[String],
    modelReleased: Option[String],
    editorNotes: Seq[String]
)
