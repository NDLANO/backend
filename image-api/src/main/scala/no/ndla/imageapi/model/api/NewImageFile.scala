/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.api

import sttp.tapir.Schema.annotations.description

@description("Url and size information about the image")
case class NewImageFile(
    @description("The name of the file") fileName: String,
    @description("ISO 639-1 code that represents the language used in the audio") language: Option[String]
)
