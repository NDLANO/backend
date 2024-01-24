/*
 * Part of NDLA search.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.search.model

import no.ndla.language.model.LanguageField
import no.ndla.mapping.ISO639

case class LanguageValue[T](language: String, value: T) extends LanguageField[T] {
  def isEmpty: Boolean = language.isEmpty
}

case class SearchableLanguageValues(languageValues: Seq[LanguageValue[String]]) {
  def map[T](f: LanguageValue[String] => T): Seq[T] = languageValues.map(lv => f(lv))

  def getLanguage(language: String): Option[String] =
    languageValues.find(_.language == language).map(_.value)

  def getLanguageOrDefault(language: String): Option[String] =
    getLanguage(language).orElse(defaultValue)

  def defaultValue: Option[String] =
    languageValues
      .sortBy(lv => ISO639.languagePriority.reverse.indexOf(lv.language))
      .lastOption
      .map(_.value)
}

object SearchableLanguageValues {

  def empty: SearchableLanguageValues = SearchableLanguageValues(Seq.empty)

  def fromFields(fields: Seq[LanguageField[String]]): SearchableLanguageValues =
    SearchableLanguageValues(fields.map(f => LanguageValue(f.language, f.value)))

  def from(values: (String, String)*): SearchableLanguageValues = {
    val languageValues = values.map { case (language, value) => LanguageValue(language, value) }
    SearchableLanguageValues(languageValues)
  }
}

object SearchableLanguageList {

  def fromFields(fields: Seq[LanguageField[Seq[String]]]): SearchableLanguageList =
    SearchableLanguageList(fields.map(f => LanguageValue(f.language, f.value)))

  def addValue(fields: SearchableLanguageList, languageValue: String): SearchableLanguageList = {
    SearchableLanguageList(
      fields.languageValues.map(field => LanguageValue(field.language, field.value :+ languageValue))
    )
  }

  def from(values: (String, Seq[String])*): SearchableLanguageList = {
    val languageValues = values.map { case (language, value) => LanguageValue(language, value) }
    SearchableLanguageList(languageValues)
  }

}

case class SearchableLanguageList(languageValues: Seq[LanguageValue[Seq[String]]]) {
  def map[T](f: LanguageValue[Seq[String]] => T): Seq[T] = languageValues.map(lv => f(lv))
}
