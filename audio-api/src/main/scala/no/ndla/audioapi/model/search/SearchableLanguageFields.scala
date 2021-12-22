/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.search

import no.ndla.audioapi.model.domain.LanguageField
import org.json4s.JsonAST.{JArray, JField, JNothing, JObject, JString}
import org.json4s.{CustomSerializer, Formats, MappingException}

case class SearchableLanguageValues(languageValues: Seq[LanguageValue[String]])

case class SearchableLanguageList(languageValues: Seq[LanguageValue[Seq[String]]])

object SearchableLanguageValues {

  def fromFields(fields: Seq[LanguageField[String]]): SearchableLanguageValues =
    SearchableLanguageValues(fields.map(f => LanguageValue(f.language, f.value)))

  class serializer
      extends CustomSerializer[SearchableLanguageValues](_ =>
        ({
          case JObject(items) =>
            SearchableLanguageValues(items.map {
              case name -> JString(value) => LanguageValue(name, value)
              case x                      => throw new MappingException(s"Cannot convert $x to SearchableLanguageValues")
            })
          case JNothing => SearchableLanguageValues(Seq.empty)
        }, {
          case x: SearchableLanguageValues =>
            JObject(
              x.languageValues.map(languageValue => JField(languageValue.lang, JString(languageValue.value))).toList)
        }))

}

object SearchableLanguageList {

  def fromFields(fields: Seq[LanguageField[Seq[String]]]): SearchableLanguageList =
    SearchableLanguageList(fields.map(f => LanguageValue(f.language, f.value)))

  class serializer
      extends CustomSerializer[SearchableLanguageList](_ =>
        ({
          case JObject(items) =>
            SearchableLanguageList(items.map {
              case JField(name, JArray(fieldItems)) =>
                LanguageValue(name, fieldItems.map {
                  case JString(value) => value
                  case x              => throw new MappingException(s"Cannot convert $x to SearchableLanguageList")
                })
              case (name, _) => throw new MappingException(s"Cannot convert $name to SearchableLanguageList")
            })
          case JNothing => SearchableLanguageList(Seq.empty)
        }, {
          case x: SearchableLanguageList =>
            JObject(
              x.languageValues
                .map(languageValue =>
                  JField(languageValue.lang, JArray(languageValue.value.map(lv => JString(lv)).toList)))
                .toList)
        }))
}

object SearchableLanguageFormats {

  val JSonFormats: Formats =
    org.json4s.DefaultFormats +
      new SearchableLanguageValues.serializer +
      new SearchableLanguageList.serializer
}
