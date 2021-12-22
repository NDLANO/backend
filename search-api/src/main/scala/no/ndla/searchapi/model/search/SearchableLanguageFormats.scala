/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import java.text.SimpleDateFormat
import java.util.TimeZone

import org.json4s.JsonAST.{JField, JObject, JString}
import org.json4s.{CustomSerializer, DefaultFormats, Extraction, Formats}

class SearchableLanguageValuesSerializer
    extends CustomSerializer[SearchableLanguageValues](_ =>
      ({
        case obj: JObject =>
          implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

          val langs = obj.values.keys
            .flatMap(l => {
              (obj \ l).extract[Option[String]].map(o => LanguageValue(l, o))
            })
            .toSeq

          SearchableLanguageValues(langs)
      }, {
        case searchableLanguageValues: SearchableLanguageValues =>
          val langValues = searchableLanguageValues.languageValues.flatMap(lv => {
            Option(lv.value)
              .map(str => JField(lv.language, JString(str)))
          })
          JObject(langValues: _*)
      }))

class SearchableLanguageListSerializer
    extends CustomSerializer[SearchableLanguageList](_ =>
      ({
        case obj: JObject =>
          implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

          val langs = obj.values.keys
            .flatMap(l => {
              (obj \ l).extract[Option[Seq[String]]].map(o => LanguageValue(l, o))
            })
            .toSeq

          SearchableLanguageList(langs)
      }, {
        case searchableLanguageList: SearchableLanguageList =>
          implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
          val langValues = searchableLanguageList.languageValues.flatMap(lv => {
            Option(lv.value).map(v => {
              val tags = Extraction.decompose(v)
              JField(lv.language, tags)
            })
          })
          JObject(langValues: _*)

      }))

object SearchableLanguageFormats {

  val defaultFormats: DefaultFormats = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
    dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"))
  }

  val JSonFormats: Formats =
    defaultFormats +
      new SearchableLanguageValuesSerializer +
      new SearchableLanguageListSerializer ++
      org.json4s.ext.JodaTimeSerializers.all
}
