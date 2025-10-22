/*
 * Part of NDLA search-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.api

import no.ndla.common.model.api.search.TitleDTO
import sttp.tapir.Schema.annotations.description

@description("Search result for image api")
case class ImageResultDTO(
    @description("The unique id of this image")
    id: Long,
    @description("The title of this image")
    title: TitleDTO,
    @description("The alt text of this image")
    altText: ImageAltTextDTO,
    @description("A direct link to the image")
    previewUrl: String,
    @description("A link to get meta data related to the image")
    metaUrl: String,
    @description("List of supported languages")
    supportedLanguages: Seq[String],
)
