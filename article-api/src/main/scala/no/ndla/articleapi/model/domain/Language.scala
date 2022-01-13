/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.domain

import com.sksamuel.elastic4s.analysis.LanguageAnalyzers
import no.ndla.articleapi.ArticleApiProperties.DefaultLanguage
import no.ndla.language.model.LanguageTag

object Language {
  val UnknownLanguage: LanguageTag = LanguageTag("und")
  val NoLanguage = ""
  val AllLanguages = "*"

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

  def findByLanguageOrBestEffort[P <: LanguageField[_]](sequence: Seq[P], language: String): Option[P] = {
    sequence
      .find(_.language == language)
      .orElse(
        sequence
          .sortBy(lf => languageAnalyzers.map(la => la.languageTag.toString).reverse.indexOf(lf.language))
          .lastOption)
  }

  def languageOrUnknown(language: Option[String]): LanguageTag = {
    language.filter(_.nonEmpty) match {
      case Some(x) if x == "unknown" => UnknownLanguage
      case Some(x)                   => LanguageTag(x)
      case None                      => UnknownLanguage
    }
  }

  def getSupportedLanguages(sequences: Seq[LanguageField[_]]*): Seq[String] = {
    sequences.flatMap(_.map(_.language)).distinct.sortBy { lang =>
      languageAnalyzers.map(la => la.languageTag).indexOf(LanguageTag(lang))
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

case class LanguageAnalyzer(languageTag: LanguageTag, analyzer: String)
