/*
 * Part of NDLA common
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.frontpage

import no.ndla.language.model.LanguageField

case class MovieTheme(name: Seq[MovieThemeName], movies: Seq[String])
case class MovieThemeName(name: String, language: String) extends LanguageField[String] {
  override def value: String    = name
  override def isEmpty: Boolean = name.isEmpty
}
