/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.language.model.LanguageField

case class ArticleIntroSummary(summary: String, language: String) extends LanguageField[String] {
  override def value: String    = summary
  override def isEmpty: Boolean = summary.isEmpty
}

object ArticleIntroSummary {
  implicit def encoder: Encoder[ArticleIntroSummary] = deriveEncoder[ArticleIntroSummary]
  implicit def decoder: Decoder[ArticleIntroSummary] = deriveDecoder[ArticleIntroSummary]
}
