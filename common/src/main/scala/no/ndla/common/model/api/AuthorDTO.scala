/*
 * Part of NDLA common
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.domain
import no.ndla.common.model.domain.ContributorType
import sttp.tapir.Schema.annotations.description

@description("Information about an author")
case class AuthorDTO(
    @description("The description of the author. Eg. Photographer or Supplier")
    `type`: ContributorType,
    @description("The name of the of the author")
    name: String
) {
  def toDomain: domain.Author = domain.Author(
    `type` = this.`type`,
    name = this.name
  )
}

object AuthorDTO {
  implicit def encoder: Encoder[AuthorDTO] = deriveEncoder[AuthorDTO]
  implicit def decoder: Decoder[AuthorDTO] = deriveDecoder[AuthorDTO]
}
