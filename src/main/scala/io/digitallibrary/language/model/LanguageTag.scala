/*
 * Part of GDL language.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.language.model

import io.digitallibrary.language.service.LanguageProvider

case class LanguageTag (language: String, script: Option[String], region: Option[String]) {

  override def toString: String = {
    Seq(Some(language), script, region).filter(_.isDefined).map(_.get).mkString("-")
  }

  def validate(implicit provider: LanguageProvider): LanguageTag = {
    provider.validate(this)
  }

  def displayName(implicit provider: LanguageProvider): String = {
    provider.validate(this)
    provider.displayName(this).getOrElse(this.toString)
  }

  def withIso639_3(implicit provider: LanguageProvider): LanguageTag = {
    provider.withIso639_3(this)
  }
}

object LanguageTag {
  def fromString(languageTagAsString: String)(implicit provider: LanguageProvider): LanguageTag = {
    LanguageTag.apply(languageTagAsString).validate.withIso639_3
  }

  def apply(languageTagAsString: String): LanguageTag = {
    val parts = languageTagAsString.split("-")
    parts.size match {
      case 1 => LanguageTag(parts(0), None, None)
      case 2 if parts(1).length == 2 => LanguageTag(parts(0), None, Option(parts(1)))
      case 2 if parts(1).length == 4 => LanguageTag(parts(0), Option(parts(1)), None)
      case 3 => LanguageTag(parts(0), Option(parts(1)), Option(parts(2)))
      case _ => throw new LanguageNotSupportedException(s"The language language tag '$languageTagAsString' is not supported.")
    }
  }
}
