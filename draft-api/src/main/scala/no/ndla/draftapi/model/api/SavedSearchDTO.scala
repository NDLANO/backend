/*
 * Part of NDLA draft-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Information about saved search")
case class SavedSearchDTO(
    @description("The search url") searchUrl: String,
    @description("The search phrase") searchPhrase: String
)

object SavedSearchDTO {
  implicit def encoder: Encoder[SavedSearchDTO] = deriveEncoder
  implicit def decoder: Decoder[SavedSearchDTO] = deriveDecoder
}
