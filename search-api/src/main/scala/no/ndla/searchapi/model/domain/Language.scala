/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain

import com.sksamuel.elastic4s.analysis.LanguageAnalyzers
import com.sksamuel.elastic4s.requests.analyzers.{
  CustomAnalyzerDefinition,
  LowercaseTokenFilter,
  StandardTokenizer,
  StemmerTokenFilter,
  StopTokenFilter
}
import no.ndla.language.model.LanguageTag
import no.ndla.searchapi.SearchApiProperties.DefaultLanguage

object Language {
  val UnknownLanguage: LanguageTag = LanguageTag("und")
  val NoLanguage = ""
  val AllLanguages = "*"
  val Nynorsk = "nynorsk"

  // Must be included in search index settings
  val nynorskLanguageAnalyzer: CustomAnalyzerDefinition = CustomAnalyzerDefinition(
    name = Nynorsk,
    tokenizer = StandardTokenizer,
    filters = Seq(LowercaseTokenFilter,
                  StopTokenFilter("norwegian_stop"),
                  StemmerTokenFilter("nynorsk_stemmer", lang = "light_nynorsk"))
  )

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

  def findByLanguageOrBestEffort[P <: LanguageField](sequence: Seq[P], language: String): Option[P] = {
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

  def getSupportedLanguages(sequences: Seq[LanguageField]*): Seq[String] = {
    sequences.flatMap(_.map(_.language)).distinct.sortBy { lang =>
      languageAnalyzers.map(la => la.languageTag.toString).indexOf(lang)
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

case class LanguageAnalyzer(languageTag: LanguageTag, analyzer: String)
