/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.search

import no.ndla.audioapi.model.domain.emptySomeToNone
import no.ndla.audioapi.model.search.LanguageValue.LanguageValue

object LanguageValue {

  case class LanguageValue[T](lang: Option[String], value: T)

  def apply[T](lang: Option[String], value: T): LanguageValue[T] = LanguageValue(emptySomeToNone(lang), value)

}


case class SearchableLanguageValues(languageValues: Seq[LanguageValue[String]])

case class SearchableLanguageList(languageValues: Seq[LanguageValue[Seq[String]]])

case class SearchableAudioInformation(
  id: String,
  titles: SearchableLanguageValues,
  tags: SearchableLanguageList,
  license: String,
  authors: Seq[String]
)
