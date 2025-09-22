/*
 * Part of NDLA common
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api.search

import io.circe.syntax.*
import io.circe.{Decoder, Encoder, FailedCursor}
import no.ndla.language.Language
import no.ndla.language.model.LanguageField

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
    Language.getDefault(languageValues).map(_.value)
}

object SearchableLanguageValues {
  implicit val encoder: Encoder[SearchableLanguageValues] = Encoder.instance { value =>
    val mapToEncode = value.languageValues.map(lv => lv.language -> lv.value).toMap
    mapToEncode.asJson
  }
  implicit val decoder: Decoder[SearchableLanguageValues] = Decoder.withReattempt {
    case c: FailedCursor if !c.incorrectFocus => Right(SearchableLanguageValues(Seq.empty))
    case c                                    =>
      c.as[Map[String, String]].map { map =>
        SearchableLanguageValues.from(map.toSeq*)
      }
  }

  def empty: SearchableLanguageValues = SearchableLanguageValues(Seq.empty)

  def fromFields(fields: Seq[LanguageField[String]]): SearchableLanguageValues =
    SearchableLanguageValues(fields.map(f => LanguageValue(f.language, f.value)))

  def fromFieldsMap[LF <: LanguageField[?]](fields: Seq[LF])(func: LF => String): SearchableLanguageValues =
    SearchableLanguageValues(fields.map(f => LanguageValue(f.language, func(f))))

  def from(values: (String, String)*): SearchableLanguageValues = {
    val languageValues = values.map { case (language, value) => LanguageValue(language, value) }
    SearchableLanguageValues(languageValues)
  }

  def combine(values: Seq[SearchableLanguageValues]): SearchableLanguageValues = {
    val allLanguages   = values.flatMap(_.map(_.language)).distinct
    val languageValues = allLanguages.map { language =>
      val valuesForLanguage = values.map(_.getLanguageOrDefault(language).getOrElse(""))
      LanguageValue(language, valuesForLanguage.mkString(" - "))
    }

    SearchableLanguageValues(languageValues)
  }
}

object SearchableLanguageList {
  implicit val encoder: Encoder[SearchableLanguageList] = Encoder.instance { value =>
    val mapToEncode = value.languageValues.map(lv => lv.language -> lv.value).toMap
    mapToEncode.asJson
  }
  implicit val decoder: Decoder[SearchableLanguageList] = Decoder.instance { cursor =>
    cursor.as[Map[String, Seq[String]]].map { map =>
      SearchableLanguageList(map.map { case (language, value) => LanguageValue(language, value) }.toSeq)
    }
  }

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
  def map[T](f: LanguageValue[Seq[String]] => T): Seq[T]                          = languageValues.map(lv => f(lv))
  def fromFields(fields: Seq[LanguageField[Seq[String]]]): SearchableLanguageList =
    SearchableLanguageList(fields.map(f => LanguageValue(f.language, f.value)))
}
