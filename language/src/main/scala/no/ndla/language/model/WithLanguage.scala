/*
 * Part of NDLA language
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.language.model

trait WithLanguage extends Ordered[WithLanguage] {
  def compare(that: WithLanguage): Int = this.language.compare(that.language)
  def language: String
}
