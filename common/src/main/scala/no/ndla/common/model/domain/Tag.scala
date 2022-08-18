/*
 * Part of NDLA common.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain

import no.ndla.language.model.LanguageField

case class Tag(tags: Seq[String], language: String) extends LanguageField[Seq[String]] {
  override def value: Seq[String] = tags
  override def isEmpty: Boolean   = tags.isEmpty
}
