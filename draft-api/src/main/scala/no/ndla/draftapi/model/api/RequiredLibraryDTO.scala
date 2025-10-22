/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.description
import no.ndla.common.DeriveHelpers

@description("Information about a library required to render the article")
case class RequiredLibraryDTO(
    @description("The type of the library. E.g. CSS or JavaScript")
    mediaType: String,
    @description("The name of the library")
    name: String,
    @description("The full url to where the library can be downloaded")
    url: String,
)

object RequiredLibraryDTO {
  implicit def encoder: Encoder[RequiredLibraryDTO] = deriveEncoder[RequiredLibraryDTO]
  implicit def decoder: Decoder[RequiredLibraryDTO] = deriveDecoder[RequiredLibraryDTO]
  implicit def schema: Schema[RequiredLibraryDTO]   = DeriveHelpers.getSchema[RequiredLibraryDTO]
}
