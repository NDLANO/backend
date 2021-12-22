/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.domain

trait LanguageField {
  def isEmpty: Boolean
  def language: String
}
