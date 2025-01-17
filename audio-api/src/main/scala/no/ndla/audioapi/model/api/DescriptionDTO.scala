/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import sttp.tapir.Schema.annotations.description

case class DescriptionDTO(
    @description("The description of the element") description: String,
    @description("ISO 639-1 code that represents the language used in the description") language: String
)
