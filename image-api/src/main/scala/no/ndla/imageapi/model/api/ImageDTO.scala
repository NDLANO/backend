/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.api

import sttp.tapir.Schema.annotations.description

@description("Url and size information about the image")
case class ImageDTO(
    @description("The full url to where the image can be downloaded") url: String,
    @description("The size of the image in bytes") size: Long,
    @description("The mimetype of the image") contentType: String
)
