/*
 * Part of NDLA search.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.search.model

import org.json4s.JsonAST.{JField, JObject, JString}
import org.json4s.{CustomSerializer, DefaultFormats, Extraction, Formats, JArray, JNothing, MappingException}

import java.text.SimpleDateFormat
import java.util.TimeZone

class SearchableLanguageValuesSerializer
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
            x.languageValues.map(languageValue => JField(languageValue.language, JString(languageValue.value))).toList)
      }))

class SearchableLanguageListSerializer
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
                JField(languageValue.language, JArray(languageValue.value.map(lv => JString(lv)).toList)))
              .toList)
      }))

object SearchableLanguageFormats {

  def defaultFormats(withMillis: Boolean): DefaultFormats = new DefaultFormats {
    val pattern: String = if (withMillis) "yyyy-MM-dd'T'HH:mm:ss.SSSX" else "yyyy-MM-dd'T'HH:mm:ssX"
    val simpleDateFormat = new SimpleDateFormat(pattern)
    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
    override def dateFormatter: SimpleDateFormat = simpleDateFormat
  }

  val JSonFormats: Formats =
    defaultFormats(false) +
      new SearchableLanguageValuesSerializer +
      new SearchableLanguageListSerializer ++
      org.json4s.ext.JodaTimeSerializers.all

  val JSonFormatsWithMillis: Formats =
    defaultFormats(true) +
      new SearchableLanguageValuesSerializer +
      new SearchableLanguageListSerializer ++
      org.json4s.ext.JodaTimeSerializers.all
}
