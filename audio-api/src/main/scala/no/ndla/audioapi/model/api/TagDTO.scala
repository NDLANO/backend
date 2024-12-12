/*
 * Part of NDLA audio-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.model.api

import sttp.tapir.Schema.annotations.description

@description("Description of the tags of the audio")
case class TagDTO(
    @description("The searchable tag.") tags: Seq[String],
    @description("ISO 639-1 code that represents the language used in tag") language: String
)
