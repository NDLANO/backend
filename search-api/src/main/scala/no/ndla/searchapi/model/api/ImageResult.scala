/*
 * Part of NDLA search-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import sttp.tapir.Schema.annotations.description

@description("Search result for image api")
case class ImageResult(
    @description("The unique id of this image") id: Long,
    @description("The title of this image") title: Title,
    @description("The alt text of this image") altText: ImageAltText,
    @description("A direct link to the image") previewUrl: String,
    @description("A link to get meta data related to the image") metaUrl: String,
    @description("List of supported languages") supportedLanguages: Seq[String]
)
