/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.learningpath

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.language.model.WithLanguage

case class Description(description: String, language: String) extends WithLanguage

object Description {
  implicit val encoder: Encoder[Description] = deriveEncoder
  implicit val decoder: Decoder[Description] = deriveDecoder
}
