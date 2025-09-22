/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.search.{SearchableLanguageList, SearchableLanguageValues}
import no.ndla.imageapi.model.domain.{ImageDimensions, ImageMetaInformation}
import no.ndla.language.model.WithLanguage

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
    domainObject: ImageMetaInformation,
    users: Seq[String]
)

object SearchableImage {
  implicit val encoder: Encoder[SearchableImage] = deriveEncoder
  implicit val decoder: Decoder[SearchableImage] = deriveDecoder
}

case class SearchableImageFile(
    imageSize: Long,
    previewUrl: String,
    fileSize: Long,
    contentType: String,
    dimensions: Option[ImageDimensions],
    language: String
) extends WithLanguage

object SearchableImageFile {
  implicit val encoder: Encoder[SearchableImageFile] = deriveEncoder
  implicit val decoder: Decoder[SearchableImageFile] = deriveDecoder
}
