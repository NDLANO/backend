/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

case class Description(description: String, language: String) extends LanguageField[String] {
  override def value: String = description
}
