/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.model.api

import sttp.tapir.Schema.annotations.description

// format: off
@description("Meta information about podcast audio")
case class PodcastMetaDTO(
  @description("Introduction for the podcast") introduction: String,
  @description("Cover photo for the podcast") coverPhoto: CoverPhotoDTO,
  @description("ISO 639-1 code that represents the language used in the title") language: String
)
// format: on
