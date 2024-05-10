/*
 * Part of NDLA draft-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.model.domain.draft.Grade
import sttp.tapir.Schema.annotations.description

@description("Quality evaluation of the article")
case class QualityEvaluation(
    @description("The grade (1-5) of the article") grade: Grade,
    @description("Note explaining the score") note: Option[String]
)

object QualityEvaluation {
  implicit def encoder: Encoder[QualityEvaluation] = deriveEncoder
  implicit def decoder: Decoder[QualityEvaluation] = deriveDecoder
}
