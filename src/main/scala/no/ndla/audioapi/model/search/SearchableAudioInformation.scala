/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.search

case class LanguageValue[T](lang: Option[String], value: T)

case class SearchableLanguageValues(languageValues: Seq[LanguageValue[String]])

case class SearchableLanguageList(languageValues: Seq[LanguageValue[Seq[String]]])

case class SearchableAudioInformation(id: String,
                                      titles: SearchableLanguageValues,
                                      tags: SearchableLanguageList,
                                      license: String,
                                      authors: Seq[String])
