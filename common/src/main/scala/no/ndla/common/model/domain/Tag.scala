/*
 * Part of NDLA common.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.language.model.LanguageField

case class Tag(tags: Seq[String], language: String) extends LanguageField[Seq[String]] {
  override def value: Seq[String] = tags
  override def isEmpty: Boolean   = tags.isEmpty
}

object Tag {
  implicit def encoder: Encoder[Tag] = deriveEncoder[Tag]
  implicit def decoder: Decoder[Tag] = deriveDecoder[Tag]
}
