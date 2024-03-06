/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Information about a library required to render the article")
case class RequiredLibrary(
    @description("The type of the library. E.g. CSS or JavaScript") mediaType: String,
    @description("The name of the library") name: String,
    @description("The full url to where the library can be downloaded") url: String
)

object RequiredLibrary {
  implicit val encoder: Encoder[RequiredLibrary] = deriveEncoder
  implicit val decoder: Decoder[RequiredLibrary] = deriveDecoder
}
