package no.ndla.language

import no.ndla.language.model.{LanguageField, LanguageTag, WithLanguage}

object Language {
  val DefaultLanguage              = "nb"
  val UnknownLanguage: LanguageTag = LanguageTag("und")
  val NoLanguage                   = ""
  val AllLanguages                 = "*"
  val Nynorsk                      = "nynorsk"

  val languagePriority = Seq(
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

  def mergeLanguageFields[A <: LanguageField[_]](existing: Seq[A], updated: Seq[A]): Seq[A] = {
    val toKeep = existing.filterNot(item => updated.map(_.language).contains(item.language))
    (toKeep ++ updated).filterNot(_.isEmpty)
  }

  def findByLanguageOrBestEffort[P <: WithLanguage](sequence: Seq[P], language: Option[String]): Option[P] = {
    language match {
      case Some(l) => findByLanguageOrBestEffort(sequence, l)
      case None    => sequence.sorted.headOption
    }
  }

  def findByLanguageOrBestEffort[P <: WithLanguage](sequence: Seq[P], language: String): Option[P] = {
    sequence
      .find(_.language == language)
      .orElse(
        sequence
          .sortBy(lf => languagePriority.reverse.indexOf(lf.language))
          .lastOption
      )
  }

  def getSupportedLanguages(sequences: Seq[WithLanguage]*): Seq[String] = {
    sequences.flatMap(_.map(_.language)).distinct.sortBy { lang =>
      languagePriority.indexOf(lang)
    }
  }

  def languageOrUnknown(language: Option[String]): LanguageTag = {
    language.filter(_.nonEmpty) match {
      case Some(x) if x == "unknown" => UnknownLanguage
      case Some(x)                   => LanguageTag(x)
      case None                      => UnknownLanguage
    }
  }

  def getSearchLanguage(languageParam: String, supportedLanguages: Seq[String]): String = {
    val l = if (languageParam == AllLanguages) DefaultLanguage else languageParam
    if (supportedLanguages.contains(l))
      l
    else
      supportedLanguages.head
  }

}
