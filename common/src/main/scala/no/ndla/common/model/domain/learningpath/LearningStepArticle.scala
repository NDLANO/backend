/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.learningpath

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.language.model.LanguageField

case class LearningStepArticle(id: Long, language: String) extends LanguageField[Long] {
  override def value: Long      = id
  override def isEmpty: Boolean = false
}

object LearningStepArticle {
  implicit val encoder: Encoder[LearningStepArticle] = deriveEncoder
  implicit val decoder: Decoder[LearningStepArticle] = deriveDecoder
}
