/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.draft

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class QualityEvaluation(
    grade: Grade,
    note: Option[String]
)

object QualityEvaluation {
  implicit def encoder: Encoder[QualityEvaluation] = deriveEncoder
  implicit def decoder: Decoder[QualityEvaluation] = deriveDecoder
}
