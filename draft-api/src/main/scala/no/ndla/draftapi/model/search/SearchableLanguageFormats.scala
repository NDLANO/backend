/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.search

import org.json4s.JsonAST.{JField, JObject, JString}
import org.json4s.{CustomSerializer, Extraction, Formats}

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
          val langValues = searchableLanguageValues.languageValues.map(lv => {
            JField(lv.language, JString(lv.value))
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
          val langValues = searchableLanguageList.languageValues.map(lv => {
            val tags = Extraction.decompose(lv.value)
            JField(lv.language, tags)
          })
          JObject(langValues: _*)

      }))

object SearchableLanguageFormats {

  val JSonFormats: Formats =
    org.json4s.DefaultFormats +
      new SearchableLanguageValuesSerializer +
      new SearchableLanguageListSerializer ++
      org.json4s.ext.JodaTimeSerializers.all
}
