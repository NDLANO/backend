/*
 * Part of NDLA backend.language.main
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.language.model

trait WithLanguageAndValue[T] extends WithLanguage {
  def language: String
  def value: T
}

case class BaseWithLanguageAndValue[T](
    language: String,
    value: T
) extends WithLanguageAndValue[T]
