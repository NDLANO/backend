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
    Seq(Some(language.id), script.map(_.code), region.map(_.code)).flatten.mkString("-").toLowerCase

  }

  def displayName: String = {
    val scriptAndRegion = (script.map(_.englishName) :: region.map(_.name) :: Nil).flatten.mkString(", ")
    if (scriptAndRegion.isEmpty) {
      language.refName
    } else {
      s"${language.refName} ($scriptAndRegion)"
    }
  }
}

object LanguageTag {
  def apply(languageTagAsString: String): LanguageTag = {
    val parts = languageTagAsString.split("-")
    val tag = parts.size match {
      case 1 => withLanguage(parts(0))
      case 2 if parts(1).length == 2 => withLanguageAndRegion(parts(0), parts(1))
      case 2 if parts(1).length == 4 => withLanguageAndScript(parts(0), parts(1))
      case 3 => withLanguageScriptAndRegion(parts(0), parts(1), parts(2))
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
