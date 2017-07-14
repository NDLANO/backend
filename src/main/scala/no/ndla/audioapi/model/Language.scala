/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model

import com.sksamuel.elastic4s.analyzers._
import no.ndla.audioapi.model.domain.{AudioMetaInformation, LanguageField, WithLanguage}

object Language {
  val DefaultLanguage = "nb"
  val UnknownLanguage = "unknown"
  val NoLanguage = ""
  val AllLanguages = "all"

  val languageAnalyzers = Seq(
    LanguageAnalyzer(DefaultLanguage, NorwegianLanguageAnalyzer),
    LanguageAnalyzer("nn", NorwegianLanguageAnalyzer),
    LanguageAnalyzer("en", EnglishLanguageAnalyzer),
    LanguageAnalyzer("fr", FrenchLanguageAnalyzer),
    LanguageAnalyzer("de", GermanLanguageAnalyzer),
    LanguageAnalyzer("es", SpanishLanguageAnalyzer),
    LanguageAnalyzer("se", StandardAnalyzer), // SAMI
    LanguageAnalyzer("zh", ChineseLanguageAnalyzer),
    LanguageAnalyzer(UnknownLanguage, StandardAnalyzer)
  )

  val supportedLanguages = languageAnalyzers.map(_.lang)

  def getSupportedLanguages(sequences: Seq[Seq[WithLanguage]]): Seq[String] = {
    sequences.flatMap(_.flatMap(_.language)).distinct
  }

  def getSupportedLanguages(aMI: AudioMetaInformation): Seq[String] = {
    getSupportedLanguages(Seq(aMI.titles, aMI.filePaths, aMI.tags))
  }

  def getSearchLanguage(languageParam: String, supportedLanguages: Seq[String]): String = {
    val l = if (languageParam == AllLanguages) DefaultLanguage else languageParam
    if (supportedLanguages.contains(l))
      l
    else
      supportedLanguages.head
  }

  def findByLanguage[T <: Any](sequence: Seq[LanguageField[T]], lang: String): Option[LanguageField[T]] = {
    sequence.find(_.language.getOrElse("") == lang)
  }

  def findValueByLanguage[T <: Any](sequence: Seq[LanguageField[T]], lang: String): Option[T] = {
    findByLanguage(sequence, lang).map(_.value)
  }
}

case class LanguageAnalyzer(lang: String, analyzer: Analyzer)
