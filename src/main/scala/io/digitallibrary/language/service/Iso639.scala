/*
 * Part of GDL language.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.language.service

import io.digitallibrary.language.model.Iso639Def

import scala.io.Source


class Iso639 {
  private val ISO639_DEFINITION = "/iso-639-3_20170202.tab"
  private val definitions = loadDefinition()

  def findAlpha3(code: String): Option[Iso639Def] = {
    code.length match {
      case 2 => definitions.find(_.part1.getOrElse("") == code)
      case 3 => definitions.find(_.id == code)
      case _ => None
    }
  }

  def loadDefinition(): Seq[Iso639Def] = {
    val lines = Source.fromInputStream(getClass.getResourceAsStream(ISO639_DEFINITION), "UTF-8").getLines().map(_.stripLineEnd.split("\t", 8)).toList
    lines.map(line =>
      Iso639Def(
        line(0),
        Option(line(1)),
        Option(line(2)),
        Option(line(3)),
        Option(line(4)),
        Option(line(5)),
        line(6),
        Option(line(7))))
  }
}




