/*
 * Part of NDLA audio-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.domain

trait WithLanguage {
  def language: String
}

trait LanguageField[T] extends WithLanguage {
  def value: T
  def language: String
}
