/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.model.api

import sttp.tapir.Schema.annotations.description

case class ManuscriptDTO(
    @description("The manuscript of the audio file") manuscript: String,
    @description("ISO 639-1 code that represents the language used in the manuscript") language: String
)
