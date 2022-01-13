/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import com.sksamuel.elastic4s.analysis.LanguageAnalyzers
import no.ndla.language.model.{Iso639, LanguageTag}
import no.ndla.learningpathapi.LearningpathApiProperties.DefaultLanguage
import no.ndla.learningpathapi.model.domain
import no.ndla.mapping.ISO639

object Language {
  val NoLanguage = ""
  val AllLanguages = "*"
  val UnknownLanguage: LanguageTag = LanguageTag("und")

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
      .orElse(sequence.sortBy(lf => ISO639.languagePriority.reverse.indexOf(lf.language)).lastOption)
  }

  def languageOrUnknown(language: Option[String]): LanguageTag = {
    language.filter(_.nonEmpty) match {
      case Some(x) if x == "unknown" => UnknownLanguage
      case Some(x)                   => LanguageTag(x)
      case None                      => UnknownLanguage
    }
  }

  def findSupportedLanguages[_](fields: Seq[LanguageField[_]]*): Seq[String] = {
    fields.flatMap(_.map(_.language)).distinct.sortBy { lang =>
      languageAnalyzers.map(la => la.languageTag.toString).indexOf(lang)
    }
  }

  def getLanguages[_](sequence: Seq[LanguageField[_]]): Seq[String] = {
    sequence.map(_.language).filterNot(l => l == Language.NoLanguage)
  }

  def findSupportedLanguages(domainLearningpath: domain.LearningPath): Seq[String] = {
    val languages = findSupportedLanguages(
      domainLearningpath.title,
      domainLearningpath.description,
      domainLearningpath.tags
    ) ++
      domainLearningpath.learningsteps.getOrElse(Seq.empty).flatMap(findSupportedLanguages)

    languages.distinct
  }

  def findSupportedLanguages(domainLearningStep: domain.LearningStep): Seq[String] = {
    findSupportedLanguages(
      domainLearningStep.title,
      domainLearningStep.description,
      domainLearningStep.embedUrl
    )
  }

  def getSearchLanguage(languageParam: String, supportedLanguages: Seq[String]): String = {
    val l =
      if (languageParam == AllLanguages) DefaultLanguage else languageParam
    if (supportedLanguages.contains(l))
      l
    else
      supportedLanguages.head
  }

  def findByLanguage[T <: Any](sequence: Seq[LanguageField[T]], lang: String): Option[LanguageField[T]] = {
    sequence.find(_.language == lang)
  }

  def findValueByLanguage[T <: Any](sequence: Seq[LanguageField[T]], lang: String): Option[T] = {
    findByLanguage(sequence, lang).map(_.value)
  }

  def getByLanguageOrHead[T <: Any](sequence: Seq[LanguageField[T]], language: String): Option[T] = {
    findValueByLanguage(sequence, language) match {
      case Some(e) => Some(e)
      case None    => sequence.headOption.map(lf => lf.value)
    }
  }
}

case class LanguageAnalyzer(languageTag: LanguageTag, analyzer: String)
