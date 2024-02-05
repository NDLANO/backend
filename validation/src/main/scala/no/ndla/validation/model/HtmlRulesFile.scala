/*
 * Part of NDLA validation
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.validation.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.validation.TagAttribute

case class Field(name: TagAttribute)

object Field {
  implicit val encoder: Encoder[Field] = deriveEncoder
  implicit val decoder: Decoder[Field] = deriveDecoder
}

case class HtmlRulesAttribute(
    fields: List[Field],
    mustContainAtLeastOneOptionalAttribute: Boolean = false
)

object HtmlRulesAttribute {
  implicit val encoder: Encoder[HtmlRulesAttribute] = deriveEncoder
  implicit val decoder: Decoder[HtmlRulesAttribute] = deriveDecoder
}

case class HtmlRulesFile(
    attributes: Map[String, HtmlRulesAttribute],
    tags: List[String]
)

object HtmlRulesFile {
  implicit val encoder: Encoder[HtmlRulesFile] = deriveEncoder
  implicit val decoder: Decoder[HtmlRulesFile] = deriveDecoder
}
