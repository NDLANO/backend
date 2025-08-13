/*
 * Part of NDLA common
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.frontpage

import no.ndla.language.model.LanguageField
import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveEncoder, deriveDecoder}

case class MovieTheme(name: Seq[MovieThemeName], movies: Seq[String])
object MovieTheme {
  implicit val encoder: Encoder[MovieTheme] = deriveEncoder
  implicit val decoder: Decoder[MovieTheme] = deriveDecoder
}
case class MovieThemeName(name: String, language: String) extends LanguageField[String] {
  override def value: String    = name
  override def isEmpty: Boolean = name.isEmpty
}
object MovieThemeName {
  implicit val encoder: Encoder[MovieThemeName] = deriveEncoder
  implicit val decoder: Decoder[MovieThemeName] = deriveDecoder
}
