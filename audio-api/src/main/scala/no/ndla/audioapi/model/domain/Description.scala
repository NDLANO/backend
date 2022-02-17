/*
 * Part of NDLA audio-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.domain

import no.ndla.language.model.LanguageField

case class Description(description: String, language: String) extends LanguageField[String] {
  override def value: String    = description
  override def isEmpty: Boolean = description.isEmpty
}
