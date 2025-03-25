/*
 * Part of NDLA language
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.language.model

trait LanguageField[T] extends WithLanguageAndValue[T] {
  def value: T
  def isEmpty: Boolean
}
