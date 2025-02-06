/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import sttp.tapir.Schema.annotations.description

// format: off
@description("Meta information about podcast audio")
case class NewPodcastMetaDTO(
  @description("Introduction for the podcast") introduction: String,
  @description("Cover photo for the podcast") coverPhotoId: String,
  @description("Cover photo alttext for the podcast") coverPhotoAltText: String,
)
// format: on
