/*
 * Part of NDLA common
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.model.api

case class Author(`type`: ContributorType, name: String) {
  def toApi: api.AuthorDTO = api.AuthorDTO(
    `type` = this.`type`,
    name = this.name
  )
}

object Author {
  implicit def encoder: Encoder[Author] = deriveEncoder[Author]
  implicit def decoder: Decoder[Author] = deriveDecoder[Author]
}
