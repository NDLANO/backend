/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.search

import no.ndla.common.model.NDLADate
import no.ndla.imageapi.model.domain.{ImageDimensions, ImageMetaInformation}
import no.ndla.language.model.WithLanguage
import no.ndla.search.model.{SearchableLanguageList, SearchableLanguageValues}

case class SearchableImage(
    id: Long,
    titles: SearchableLanguageValues,
    alttexts: SearchableLanguageValues,
    captions: SearchableLanguageValues,
    tags: SearchableLanguageList,
    contributors: Seq[String],
    license: String,
    lastUpdated: NDLADate,
    defaultTitle: Option[String],
    modelReleased: Option[String],
    editorNotes: Seq[String],
    imageFiles: Seq[SearchableImageFile],
    podcastFriendly: Boolean,
    domainObject: ImageMetaInformation
)

case class SearchableImageFile(
    imageSize: Long,
    previewUrl: String,
    fileSize: Long,
    contentType: String,
    dimensions: Option[ImageDimensions],
    language: String
) extends WithLanguage
