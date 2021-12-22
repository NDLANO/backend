/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.domain

case class MetaDescription(metaDescription: String, language: String) extends LanguageField {
  override def isEmpty: Boolean = metaDescription.isEmpty
}
