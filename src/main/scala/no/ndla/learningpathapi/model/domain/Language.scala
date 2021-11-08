/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import com.sksamuel.elastic4s.analyzers._
import no.ndla.learningpathapi.LearningpathApiProperties.DefaultLanguage
import no.ndla.learningpathapi.model.domain
import no.ndla.mapping.ISO639

object Language {
  val CHINESE = "zh"
  val ENGLISH = "en"
  val FRENCH = "fr"
  val GERMAN = "de"
  val NORWEGIAN_BOKMAL = "nb"
  val NORWEGIAN_NYNORSK = "nn"
  val SAMI = "se"
  val SPANISH = "es"
  val UNKNOWN = "unknown"

  val NoLanguage = ""
  val AllLanguages = "all"
  val UnknownLanguage = "unknown"

  val languageAnalyzers = Seq(
    LanguageAnalyzer(NORWEGIAN_BOKMAL, NorwegianLanguageAnalyzer),
    LanguageAnalyzer(NORWEGIAN_NYNORSK, NorwegianLanguageAnalyzer),
    LanguageAnalyzer(ENGLISH, EnglishLanguageAnalyzer),
    LanguageAnalyzer(FRENCH, FrenchLanguageAnalyzer),
    LanguageAnalyzer(GERMAN, GermanLanguageAnalyzer),
    LanguageAnalyzer(SPANISH, SpanishLanguageAnalyzer),
    LanguageAnalyzer(SAMI, StandardAnalyzer), // SAMI
    LanguageAnalyzer(CHINESE, ChineseLanguageAnalyzer),
    LanguageAnalyzer(UnknownLanguage, NorwegianLanguageAnalyzer)
  )

  private val supportedLanguages = languageAnalyzers.map(_.lang)

  def findByLanguageOrBestEffort[P <: LanguageField[_]](sequence: Seq[P], language: String): Option[P] = {
    sequence
      .find(_.language == language)
      .orElse(sequence.sortBy(lf => ISO639.languagePriority.reverse.indexOf(lf.language)).lastOption)
  }

  def getLanguageOrDefaultIfUnsupported(language: String): String =
    supportedLanguages.find(_ == language.toLowerCase()).getOrElse(AllLanguages)

  def languageOrUnknown(language: String): String =
    languageOrUnknown(Option(language))

  def languageOrUnknown(language: Option[String]): String = {
    language match {
      case Some(lang) if ISO639.languagePriority.contains(lang) => lang
      case _                                                    => UnknownLanguage
    }
  }

  def findSupportedLanguages[_](fields: Seq[LanguageField[_]]*): Seq[String] = {
    fields.map(getLanguages).reduce(_ concat _).distinct.sortBy { lang =>
      ISO639.languagePriority.indexOf(lang)
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

case class LanguageAnalyzer(lang: String, analyzer: Analyzer)
