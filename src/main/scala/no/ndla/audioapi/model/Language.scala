/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model

import com.sksamuel.elastic4s.analyzers._
import no.ndla.audioapi.AudioApiProperties.DefaultLanguage
import no.ndla.audioapi.model.domain.{LanguageField, WithLanguage}
import no.ndla.mapping.ISO639

import scala.annotation.tailrec

object Language {
  val UnknownLanguage = "unknown"
  val AllLanguages = "all"
  val NoLanguage = ""

  val languageAnalyzers = Seq(
    LanguageAnalyzer("nb", NorwegianLanguageAnalyzer),
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

  val supportedLanguages: Seq[String] = languageAnalyzers.map(_.lang)

  def findByLanguageOrBestEffort[P <: WithLanguage](sequence: Seq[P], lang: Option[String]): Option[P] = {
    @tailrec
    def findFirstLanguageMatching(sequence: Seq[P], lang: Seq[String]): Option[P] = {
      lang match {
        case Nil => sequence.headOption
        case head :: tail =>
          sequence.find(_.language == head) match {
            case Some(x) => Some(x)
            case None    => findFirstLanguageMatching(sequence, tail)
          }
      }
    }

    findFirstLanguageMatching(sequence, lang.toList :+ DefaultLanguage)
  }

  def findLanguagePrioritized[P <: WithLanguage](sequence: Seq[P], language: String): Option[P] = {
    sequence
      .find(_.language == language)
      .orElse(sequence.sortBy(lf => ISO639.languagePriority.reverse.indexOf(lf.language)).lastOption)
  }

  def findByLanguage[T](sequence: Seq[LanguageField[T]], lang: String): Option[T] =
    sequence.find(_.language == lang).map(_.value)

  def languageOrUnknown(language: Option[String]): String = {
    language.filter(_.nonEmpty) match {
      case Some(x) => x
      case None    => UnknownLanguage
    }
  }

  def getSupportedLanguages[P <: WithLanguage](sequences: Seq[P]*): Seq[String] = {
    sequences.flatMap(_.map(_.language)).distinct.sortBy { lang =>
      ISO639.languagePriority.indexOf(lang)
    }
  }
}

case class LanguageAnalyzer(lang: String, analyzer: Analyzer)
