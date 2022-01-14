/*
 * Part of NDLA search.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.search

import com.sksamuel.elastic4s.analysis.{
  CustomAnalyzer,
  LanguageAnalyzers,
  StandardTokenizer,
  StemmerTokenFilter,
  StopTokenFilter
}
import no.ndla.language.model.LanguageTag

object Language {
  val DefaultLanguage = "nb"
  val UnknownLanguage: LanguageTag = LanguageTag("und")
  val NoLanguage = ""
  val AllLanguages = "*"
  val Nynorsk = "nynorsk"

  val tokenFilters = List(
    StopTokenFilter("norwegian_stop", language = Some("norwegian")),
    StemmerTokenFilter("nynorsk_stemmer", lang = "light_nynorsk")
  )

  // Must be included in search index settings
  val NynorskLanguageAnalyzer: CustomAnalyzer = CustomAnalyzer(
    name = Nynorsk,
    tokenizer = "standard",
    tokenFilters = List(
      "lowercase",
      "norwegian_stop",
      "nynorsk_stemmer"
    )
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
}

case class LanguageAnalyzer(languageTag: LanguageTag, analyzer: String)
