/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

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
  implicit def encoder: Encoder[RequiredLibrary] = deriveEncoder[RequiredLibrary]
  implicit def decoder: Decoder[RequiredLibrary] = deriveDecoder[RequiredLibrary]
}
