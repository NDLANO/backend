/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain

import com.sksamuel.elastic4s.analyzers._
import no.ndla.mapping.ISO639
import scala.annotation.tailrec

object Language {
  val DefaultLanguage = "nb"
  val UnknownLanguage = "unknown"
  val NoLanguage = ""
  val AllLanguages = "all"

  val languageAnalyzers = List(
    LanguageAnalyzer(DefaultLanguage, NorwegianLanguageAnalyzer),
    LanguageAnalyzer("nn", NorwegianLanguageAnalyzer),
    LanguageAnalyzer("en", EnglishLanguageAnalyzer),
    LanguageAnalyzer("fr", FrenchLanguageAnalyzer),
    LanguageAnalyzer("de", GermanLanguageAnalyzer),
    LanguageAnalyzer("es", SpanishLanguageAnalyzer),
    LanguageAnalyzer("se", StandardAnalyzer), // SAMI
    LanguageAnalyzer("sma", StandardAnalyzer), // SAMI
    LanguageAnalyzer("zh", ChineseLanguageAnalyzer),
    LanguageAnalyzer(UnknownLanguage, StandardAnalyzer)
  )

  val supportedLanguages = languageAnalyzers.map(_.lang)

  def findByLanguageOrBestEffort[P <: LanguageField](sequence: Seq[P], language: String): Option[P] = {
    sequence
      .find(_.language == language)
      .orElse(sequence.sortBy(lf => ISO639.languagePriority.reverse.indexOf(lf.language)).lastOption)
  }

  def languageOrUnknown(language: Option[String]): String = {
    language.filter(_.nonEmpty) match {
      case Some(x) => x
      case None    => UnknownLanguage
    }
  }

  def getSupportedLanguages(sequences: Seq[LanguageField]*): Seq[String] = {
    sequences.flatMap(_.map(_.language)).distinct.sortBy { lang =>
      ISO639.languagePriority.indexOf(lang)
    }
  }

  def getSearchLanguage(languageParam: String, supportedLanguages: Seq[String]): String = {
    val l = if (languageParam == AllLanguages) DefaultLanguage else languageParam
    if (supportedLanguages.contains(l))
      l
    else
      supportedLanguages.head
  }

  def findByLanguage[T](sequence: Seq[LanguageField], lang: String): Option[LanguageField] = {
    sequence.find(_.language == lang)
  }
}

case class LanguageAnalyzer(lang: String, analyzer: Analyzer)
