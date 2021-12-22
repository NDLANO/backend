/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.domain

object Language {
  val UnknownLanguage = "und"
  val NoLanguage = ""
  val AllLanguages = "*"

  // Same as in other apis, but here we have no search-engine
  val languages = Seq(
    "nb",
    "nn",
    "sma",
    "se",
    "en",
    "ar",
    "hy",
    "eu",
    "pt-br",
    "bg",
    "ca",
    "ja",
    "ko",
    "zh",
    "cs",
    "da",
    "nl",
    "fi",
    "fr",
    "gl",
    "de",
    "el",
    "hi",
    "hu",
    "id",
    "ga",
    "it",
    "lt",
    "lv",
    "fa",
    "pt",
    "ro",
    "ru",
    "srb",
    "es",
    "sv",
    "th",
    "tr",
    "und"
  )

  def getSupportedLanguages(sequences: Seq[Seq[LanguageField]]): Seq[String] = {
    sequences.flatMap(_.map(_.language)).distinct.sortBy { lang =>
      languages.indexOf(lang)
    }
  }

  def findByLanguageOrBestEffort[P <: LanguageField](sequence: Seq[P], language: String): Option[P] = {
    sequence
      .find(_.language == language)
      .orElse(sequence.sortBy(lf => languages.indexOf(lf.language)).lastOption)
  }
}
