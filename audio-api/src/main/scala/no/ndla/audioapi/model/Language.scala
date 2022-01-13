/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model

import com.sksamuel.elastic4s.analysis.LanguageAnalyzers
import no.ndla.audioapi.AudioApiProperties.DefaultLanguage
import no.ndla.audioapi.model.domain.{LanguageField, WithLanguage}
import no.ndla.language.model.LanguageTag

import scala.annotation.tailrec

object Language {
  val UnknownLanguage: LanguageTag = LanguageTag("und") // Undefined
  val DefaultLang: LanguageTag = LanguageTag(DefaultLanguage)
  val AllLanguages = "*"
  val NoLanguage = ""
  val standardAnalyzer = "standard"

  val languageAnalyzers = Seq(
    LanguageAnalyzer(LanguageTag("nb"), LanguageAnalyzers.norwegian),
    LanguageAnalyzer(LanguageTag("nn"), LanguageAnalyzers.norwegian),
    LanguageAnalyzer(LanguageTag("sma"), standardAnalyzer), // Southern sami
    LanguageAnalyzer(LanguageTag("se"), standardAnalyzer), // Northern Sami
    LanguageAnalyzer(LanguageTag("en"), LanguageAnalyzers.english),
    LanguageAnalyzer(LanguageTag("ar"), LanguageAnalyzers.arabic),
    LanguageAnalyzer(LanguageTag("hy"), LanguageAnalyzers.armenian),
    LanguageAnalyzer(LanguageTag("eu"), LanguageAnalyzers.basque),
    LanguageAnalyzer(LanguageTag("pt-br"), LanguageAnalyzers.brazilian),
    LanguageAnalyzer(LanguageTag("bg"), LanguageAnalyzers.bulgarian),
    LanguageAnalyzer(LanguageTag("ca"), LanguageAnalyzers.catalan),
    LanguageAnalyzer(LanguageTag("ja"), LanguageAnalyzers.cjk),
    LanguageAnalyzer(LanguageTag("ko"), LanguageAnalyzers.cjk),
    LanguageAnalyzer(LanguageTag("zh"), LanguageAnalyzers.cjk),
    LanguageAnalyzer(LanguageTag("cs"), LanguageAnalyzers.czech),
    LanguageAnalyzer(LanguageTag("da"), LanguageAnalyzers.danish),
    LanguageAnalyzer(LanguageTag("nl"), LanguageAnalyzers.dutch),
    LanguageAnalyzer(LanguageTag("fi"), LanguageAnalyzers.finnish),
    LanguageAnalyzer(LanguageTag("fr"), LanguageAnalyzers.french),
    LanguageAnalyzer(LanguageTag("gl"), LanguageAnalyzers.galician),
    LanguageAnalyzer(LanguageTag("de"), LanguageAnalyzers.german),
    LanguageAnalyzer(LanguageTag("el"), LanguageAnalyzers.greek),
    LanguageAnalyzer(LanguageTag("hi"), LanguageAnalyzers.hindi),
    LanguageAnalyzer(LanguageTag("hu"), LanguageAnalyzers.hungarian),
    LanguageAnalyzer(LanguageTag("id"), LanguageAnalyzers.indonesian),
    LanguageAnalyzer(LanguageTag("ga"), LanguageAnalyzers.irish),
    LanguageAnalyzer(LanguageTag("it"), LanguageAnalyzers.italian),
    LanguageAnalyzer(LanguageTag("lt"), LanguageAnalyzers.lithuanian),
    LanguageAnalyzer(LanguageTag("lv"), LanguageAnalyzers.latvian),
    LanguageAnalyzer(LanguageTag("fa"), LanguageAnalyzers.persian),
    LanguageAnalyzer(LanguageTag("pt"), LanguageAnalyzers.portuguese),
    LanguageAnalyzer(LanguageTag("ro"), LanguageAnalyzers.romanian),
    LanguageAnalyzer(LanguageTag("ru"), LanguageAnalyzers.russian),
    LanguageAnalyzer(LanguageTag("srb"), LanguageAnalyzers.sorani),
    LanguageAnalyzer(LanguageTag("es"), LanguageAnalyzers.spanish),
    LanguageAnalyzer(LanguageTag("sv"), LanguageAnalyzers.swedish),
    LanguageAnalyzer(LanguageTag("th"), LanguageAnalyzers.thai),
    LanguageAnalyzer(LanguageTag("tr"), LanguageAnalyzers.turkish),
    LanguageAnalyzer(UnknownLanguage, standardAnalyzer)
  )

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
      .orElse(
        sequence
          .sortBy(lf => languageAnalyzers.map(la => la.languageTag.toString()).reverse.indexOf(lf.language))
          .lastOption)
  }

  def findByLanguage[T](sequence: Seq[LanguageField[T]], lang: String): Option[T] =
    sequence.find(_.language == lang).map(_.value)

  def languageOrUnknown(language: Option[String]): LanguageTag = {
    language.filter(_.nonEmpty) match {
      case Some(x) if x == "unknown" => UnknownLanguage
      case Some(x)                   => LanguageTag(x)
      case None                      => UnknownLanguage
    }
  }

  def getSupportedLanguages[P <: WithLanguage](sequences: Seq[P]*): Seq[String] = {
    sequences.flatMap(_.map(_.language)).distinct.sortBy { lang =>
      languageAnalyzers.map(la => la.languageTag).indexOf(LanguageTag(lang))
    }
  }
}

case class LanguageAnalyzer(languageTag: LanguageTag, analyzer: String)
