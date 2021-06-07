/*
 * Part of NDLA audio-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.domain

case class Description(description: String, language: String) extends LanguageField[String] {
  override def value: String = description
}
