/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import no.ndla.learningpathapi.model.domain

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

  val DefaultLanguage = NORWEGIAN_BOKMAL
  val NoLanguage = ""
  val AllLanguages = "all"
  val UnknownLanguage = "unknown"

  def findByLanguageOrBestEffort[P <: LanguageField[_]](sequence: Seq[P], lang: Option[String]): Option[P] = {
    def findFirstLanguageMatching(sequence: Seq[P], lang: Seq[String]): Option[P] = {
      lang match {
        case Nil => sequence.headOption
        case head :: tail =>
          sequence.find(_.language == head) match {
            case Some(x) => Some(x)
            case None => findFirstLanguageMatching(sequence, tail)
          }
      }
    }

    findFirstLanguageMatching(sequence, lang.toList :+ DefaultLanguage)
  }

  def languageOrUnknown(language: String): String = languageOrUnknown(Option(language))

  def languageOrUnknown(language: Option[String]): String = {
    language.filter(_.nonEmpty) match {
      case Some(x) => x
      case None => UnknownLanguage
    }
  }

  def findSupportedLanguages[_](fields: Seq[LanguageField[_]]*): Seq[String] = {
    fields.map(getLanguages).reduce(_ union _).distinct
  }

  def getLanguages[_](sequence: Seq[LanguageField[_]]): Seq[String] = {
    sequence.map(_.language).filterNot(l => l == Language.NoLanguage)
  }

  def findSupportedLanguages(domainLearningpath: domain.LearningPath): Seq[String] = {
    val languages =
      getLanguages(domainLearningpath.title) ++
        getLanguages(domainLearningpath.description) ++
        getLanguages(domainLearningpath.tags) ++
        domainLearningpath.learningsteps.flatMap(findSupportedLanguages)

    languages.distinct
  }

  def findSupportedLanguages(domainLearningStep: domain.LearningStep): Seq[String] = {
    val languages =
      getLanguages(domainLearningStep.title) ++
        getLanguages(domainLearningStep.description) ++
        getLanguages(domainLearningStep.embedUrl)

    languages.distinct
  }

  def getSearchLanguage(languageParam: String, supportedLanguages: Seq[String]): String = {
    val l = if (languageParam == AllLanguages) DefaultLanguage else languageParam
    if (supportedLanguages.contains(l))
      l
    else
      supportedLanguages.head
  }

  def findByLanguage[T <: LanguageField[_]](sequence: Seq[T], lang: String): Option[T] = {
    sequence.find(_.language == lang)
  }

}

