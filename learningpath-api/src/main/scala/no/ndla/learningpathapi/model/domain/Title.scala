/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import no.ndla.language.model.{LanguageField, WithLanguage}

case class Title(title: String, language: String) extends LanguageField[String] {
  override def value: String = title
  override def isEmpty: Boolean = title.isEmpty
}
