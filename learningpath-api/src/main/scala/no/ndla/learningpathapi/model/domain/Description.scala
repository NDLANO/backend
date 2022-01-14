/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import no.ndla.language.model.LanguageField

case class Description(description: String, language: String) extends LanguageField[String] {
  override def value: String = description
  override def isEmpty: Boolean = description.isEmpty
}
