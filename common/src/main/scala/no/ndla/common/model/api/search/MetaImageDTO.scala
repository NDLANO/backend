/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.language.model.LanguageField
import sttp.tapir.Schema.annotations.description

@description("Meta image of the resource")
case class MetaImageDTO(
    @description("The meta image id") url: String,
    @description("The meta image alt text") alt: String,
    @description("The ISO 639-1 language code describing which translation this meta image belongs to") language: String
) extends LanguageField[(String, String)] {
  override def value: (String, String) = url -> alt
  override def isEmpty: Boolean        = url.isEmpty || alt.isEmpty
}

object MetaImageDTO {
  implicit val encoder: Encoder[MetaImageDTO] = deriveEncoder
  implicit val decoder: Decoder[MetaImageDTO] = deriveDecoder
}
