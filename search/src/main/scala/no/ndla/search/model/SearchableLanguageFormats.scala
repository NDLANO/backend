/*
 * Part of NDLA search.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.search.model

import enumeratum.Json4s
import no.ndla.common.model.domain.draft.RevisionStatus
import org.json4s.JsonAST.{JField, JObject, JString}
import org.json4s.ext.{JavaTimeSerializers, JavaTypesSerializers}
import org.json4s.{CustomSerializer, DefaultFormats, Formats, JArray, JNothing, MappingException}

import java.util.TimeZone

class SearchableLanguageValuesSerializer
    extends CustomSerializer[SearchableLanguageValues](_ =>
      (
        {
          case JObject(items) =>
            SearchableLanguageValues(items.map {
              case name -> JString(value) => LanguageValue(name, value)
              case x => throw new MappingException(s"Cannot convert $x to SearchableLanguageValues")
            })
          case JNothing => SearchableLanguageValues(Seq.empty)
        },
        { case x: SearchableLanguageValues =>
          JObject(
            x.languageValues.map(languageValue => JField(languageValue.language, JString(languageValue.value))).toList
          )
        }
      )
    )

class SearchableLanguageListSerializer
    extends CustomSerializer[SearchableLanguageList](_ =>
      (
        {
          case JObject(items) =>
            SearchableLanguageList(items.map {
              case JField(name, JArray(fieldItems)) =>
                LanguageValue(
                  name,
                  fieldItems.map {
                    case JString(value) => value
                    case x              => throw new MappingException(s"Cannot convert $x to SearchableLanguageList")
                  }
                )
              case (name, _) => throw new MappingException(s"Cannot convert $name to SearchableLanguageList")
            })
          case JNothing => SearchableLanguageList(Seq.empty)
        },
        { case x: SearchableLanguageList =>
          JObject(
            x.languageValues
              .map(languageValue =>
                JField(languageValue.language, JArray(languageValue.value.map(lv => JString(lv)).toList))
              )
              .toList
          )
        }
      )
    )

object SearchableLanguageFormats {

  def defaultFormats(withMillis: Boolean): DefaultFormats = new DefaultFormats {
    dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"))

    if (withMillis) {
      dateFormatter.applyPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    }
  }

  val JSonFormats: Formats =
    defaultFormats(false) +
      new SearchableLanguageValuesSerializer +
      new SearchableLanguageListSerializer ++
      JavaTimeSerializers.all ++
      JavaTypesSerializers.all +
      Json4s.serializer(RevisionStatus)

  val JSonFormatsWithMillis: Formats =
    defaultFormats(true) +
      new SearchableLanguageValuesSerializer +
      new SearchableLanguageListSerializer ++
      JavaTimeSerializers.all ++
      JavaTypesSerializers.all +
      Json4s.serializer(RevisionStatus)
}
