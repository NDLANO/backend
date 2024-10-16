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

case class ArticleIntroSummary(content: String, language: String) extends LanguageField[String] {
  override def value: String    = content
  override def isEmpty: Boolean = content.isEmpty
}

object ArticleIntroSummary {
  implicit def encoder: Encoder[ArticleContent] = deriveEncoder[ArticleContent]
  implicit def decoder: Decoder[ArticleContent] = deriveDecoder[ArticleContent]
}
