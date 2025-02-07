/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import sttp.tapir.Schema.annotations.description

@description("Meta information about podcast audio")
case class CoverPhotoDTO(
    @description("Id for the coverPhoto in image-api") id: String,
    @description("Url to the coverPhoto") url: String,
    @description("Alttext for the coverPhoto") altText: String
)
