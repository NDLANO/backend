/*
 * Part of GDL language.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.language.model

import io.digitallibrary.language.model.CodeLists.{Iso15924, Iso3166, Iso639}

import scala.util.{Failure, Try}

case class LanguageTag (language: Iso639, script: Option[Iso15924], region: Option[Iso3166]) {

  override def toString: String = {
    Seq(Some(language.part1.getOrElse(language.id)), script.map(_.code), region.map(_.code)).flatten.mkString("-").toLowerCase

  }

  def displayName: String = {
    val scriptAndRegion = (script.map(_.englishName) :: region.map(_.name) :: Nil).flatten.mkString(", ")
    if (scriptAndRegion.isEmpty) {
      language.refName
    } else {
      s"${language.refName} ($scriptAndRegion)"
    }
  }

  def localDisplayName: Option[String] = {
    language.localName.map(languageName => {
      val scriptAndRegion = (script.map(_.englishName) :: region.map(_.name) :: Nil).flatten.mkString(", ")
      if (scriptAndRegion.isEmpty) {
        languageName
      } else {
        s"$languageName ($scriptAndRegion)"
      }
    })
  }
}

object LanguageTag {
  def apply(languageTagAsString: String): LanguageTag = {
    val tag = languageTagAsString.split("-").toList match {
      case lang :: Nil => withLanguage(lang)
      case lang :: region :: Nil if region.length == 2 => withLanguageAndRegion(lang, region)
      case lang :: script :: Nil if script.length == 4 => withLanguageAndScript(lang, script)
      case lang :: script :: region :: Nil => withLanguageScriptAndRegion(lang, script, region)
      case _ => Failure(new LanguageNotSupportedException(s"The language tag '$languageTagAsString' is not supported."))
    }

    tag.get //throws the exception if it is a failure.
  }

  private def withLanguageScriptAndRegion(language: String, script: String, region: String): Try[LanguageTag] = {
    for {
      iso639 <- Iso639.get(language)
      iso3166 <- Iso3166.get(region)
      iso15924 <- Iso15924.get(script)
    } yield LanguageTag(iso639, Some(iso15924), Some(iso3166))
  }

  private def withLanguageAndScript(language: String, script: String): Try[LanguageTag] = {
    for {
      iso639 <- Iso639.get(language)
      iso15924 <- Iso15924.get(script)
    } yield LanguageTag(iso639, Some(iso15924), None)
  }

  private def withLanguageAndRegion(language: String, region: String): Try[LanguageTag] = {
    for {
      iso639 <- Iso639.get(language)
      iso3166 <- Iso3166.get(region)
    } yield LanguageTag(iso639, None, Some(iso3166))
  }

  private def withLanguage(language: String): Try[LanguageTag] = {
    Iso639.get(language).map(LanguageTag(_, None, None))
  }
}
