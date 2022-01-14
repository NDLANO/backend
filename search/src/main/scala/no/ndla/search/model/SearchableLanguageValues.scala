/*
 * Part of NDLA search.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.search.model

import no.ndla.language.model.LanguageField

case class LanguageValue[T](language: String, value: T) extends LanguageField[T] {
  def isEmpty: Boolean = language.isEmpty
}

case class SearchableLanguageValues(languageValues: Seq[LanguageValue[String]])

object SearchableLanguageValues {

  def fromFields(fields: Seq[LanguageField[String]]): SearchableLanguageValues =
    SearchableLanguageValues(fields.map(f => LanguageValue(f.language, f.value)))
}

object SearchableLanguageList {

  def fromFields(fields: Seq[LanguageField[Seq[String]]]): SearchableLanguageList =
    SearchableLanguageList(fields.map(f => LanguageValue(f.language, f.value)))
}

case class SearchableLanguageList(languageValues: Seq[LanguageValue[Seq[String]]])
