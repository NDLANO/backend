/*
 * Part of NDLA search-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import sttp.tapir.Schema.annotations.description

@description("Search result for audio api")
case class AudioResult(
    @description("The unique id of this audio") id: Long,
    @description("The title of this audio") title: Title,
    @description("A direct link to the audio") url: String,
    @description("List of supported languages") supportedLanguages: Seq[String]
)
