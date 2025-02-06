/*
 * Part of NDLA audio-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import sttp.tapir.Schema.annotations.description

case class TitleDTO(
    @description("The title of the audio file") title: String,
    @description("ISO 639-1 code that represents the language used in the title") language: String
)
