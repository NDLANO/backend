/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.search

import java.util.Date

object LanguageValue {

  case class LanguageValue[T](lang: String, value: T)

  def apply[T](lang: String, value: T): LanguageValue[T] = LanguageValue(lang, value)

}

case class SearchableLanguageValues(languageValues: Seq[LanguageValue.LanguageValue[String]])

case class SearchableLanguageList(languageValues: Seq[LanguageValue.LanguageValue[Seq[String]]])

// TODO: Add podcastmeta here for searching for podcast descriptions
case class SearchableAudioInformation(
    id: String,
    titles: SearchableLanguageValues,
    tags: SearchableLanguageList,
    license: String,
    authors: Seq[String],
    lastUpdated: Date,
    defaultTitle: Option[String],
    audioType: String
)
