/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.domain

trait LanguageField extends Ordered[LanguageField] {
  def compare(that: LanguageField): Int = this.language.compare(that.language)
  def isEmpty: Boolean
  def language: String
}
