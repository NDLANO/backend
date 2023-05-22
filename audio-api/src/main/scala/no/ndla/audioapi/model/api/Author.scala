/*
 * Part of NDLA audio-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.model.api

import sttp.tapir.Schema.annotations.description

case class Author(
    @description("The description of the author. Eg. author or publisher") `type`: String,
    @description("The name of the of the author") name: String
)
