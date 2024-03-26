/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.domain

import no.ndla.language.model.LanguageField

case class MetaDescription(metaDescription: String, language: String) extends LanguageField[String] {
  override def isEmpty: Boolean = metaDescription.isEmpty
  override def value: String    = metaDescription
}
