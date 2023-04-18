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

case class SearchableLanguageValues(languageValues: Seq[LanguageValue[String]]) {
  def map[T](f: LanguageValue[String] => T): Seq[T] = languageValues.map(lv => f(lv))
}

object SearchableLanguageValues {

  def fromFields(fields: Seq[LanguageField[String]]): SearchableLanguageValues =
    SearchableLanguageValues(fields.map(f => LanguageValue(f.language, f.value)))
}

object SearchableLanguageList {

  def fromFields(fields: Seq[LanguageField[Seq[String]]]): SearchableLanguageList =
    SearchableLanguageList(fields.map(f => LanguageValue(f.language, f.value)))

  def addValue(fields: SearchableLanguageList, languageValue: String): SearchableLanguageList = {
    SearchableLanguageList(
      fields.languageValues.map(field => LanguageValue(field.language, field.value :+ languageValue))
    )
  }

}

case class SearchableLanguageList(languageValues: Seq[LanguageValue[Seq[String]]]) {
  def map[T](f: LanguageValue[Seq[String]] => T): Seq[T] = languageValues.map(lv => f(lv))
}
