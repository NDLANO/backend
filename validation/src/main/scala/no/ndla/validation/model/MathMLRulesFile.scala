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

case class MathMLRulesFile(attributes: Map[String, List[TagAttribute]])

object MathMLRulesFile {
  implicit val encoder: Encoder[MathMLRulesFile] = deriveEncoder
  implicit val decoder: Decoder[MathMLRulesFile] = deriveDecoder
}
