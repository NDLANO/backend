/*
 * Part of GDL language.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.language.service

import io.digitallibrary.language.model._

import scala.util.{Failure, Success, Try}

class LanguageProvider(iso639: Iso639, iso3166: Iso3166, iso15924: Iso15924) {

  @throws[LanguageNotSupportedException]
  def validate(languageTag: LanguageTag): LanguageTag = {
    val validated = for {
      _ <- validIso639(languageTag.language)
      _ <- validIso15924(languageTag.script)
      _ <- validIso3166(languageTag.region)
    } yield languageTag

    validated.get
  }

  def displayName(tag: LanguageTag): Option[String] = {
    val language = iso639.findAlpha3(tag.language).map(_.refName)
    val script = tag.script.flatMap(iso15924.findAlpha4).map(_.englishName)
    val region = tag.region.flatMap(iso3166.findAlpha2).map(_.name)

    val scriptAndRegion = (script :: region :: Nil).flatten.mkString(", ")
    if (scriptAndRegion.isEmpty) {
      language
    } else {
      language.map(lang => s"$lang ($scriptAndRegion)")
    }
  }

  def withIso639_3(languageTag: LanguageTag): LanguageTag = {
    iso639.findAlpha3(languageTag.language).map(_.id) match {
      case Some(code) => languageTag.copy(language = code)
      case None => throw new LanguageSubtagNotSupportedException(languageTag.language)
    }
  }

  private def validIso639(code: String): Try[Unit] = {
    iso639.findAlpha3(code) match {
      case Some(x) => Success()
      case None => Failure(new LanguageSubtagNotSupportedException(code))
    }
  }

  private def validIso15924(scriptOpt: Option[String]): Try[Unit] = {
    scriptOpt match {
      case None => Success()
      case Some(script) => iso15924.findAlpha4(script) match {
        case None => Failure(new ScriptSubtagNotSupportedException(script))
        case Some(_) => Success()
      }
    }
  }

  private def validIso3166(regionOpt: Option[String]): Try[Unit] = {
    regionOpt match {
      case None => Success()
      case Some(region) => iso3166.findAlpha2(region) match {
        case None => Failure(new RegionSubtagNotSupportedException(region))
        case Some(_) => Success()
      }
    }
  }
}


