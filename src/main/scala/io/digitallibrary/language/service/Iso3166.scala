/*
 * Part of GDL language.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.language.service

import io.digitallibrary.language.model.Iso3166Def
import org.json4s.Formats
import org.json4s.native.Serialization.read

import scala.io.Source

class Iso3166 {
  private val ISO3166_DEFINITION = "/iso-3166-2.json"
  private val definitions = loadDefinitions()

  def findAlpha2(code: String): Option[Iso3166Def] = {
    definitions.find(_.code.toLowerCase == code.toLowerCase)
  }

  def loadDefinitions(): Seq[Iso3166Def] = {
    implicit val formats: Formats = org.json4s.DefaultFormats + Iso3166Def.Serializer
    read[Seq[Iso3166Def]](Source.fromInputStream(getClass.getResourceAsStream(ISO3166_DEFINITION), "UTF-8").mkString)
  }
}
