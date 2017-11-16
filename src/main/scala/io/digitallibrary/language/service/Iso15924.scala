/*
 * Part of GDL language.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.language.service

import io.digitallibrary.language.model.Iso15924Def

import scala.io.Source


class Iso15924 {
  private val ISO15924_DEFINITION = "/iso15924-utf8-20170726.txt"
  private val definitions = loadDefinitions()

  def findAlpha4(code: String): Option[Iso15924Def] = {
    definitions.find(_.code.toLowerCase == code.toLowerCase)
  }

  def loadDefinitions(): Seq[Iso15924Def] = {
    val lines = Source.fromInputStream(getClass.getResourceAsStream(ISO15924_DEFINITION), "UTF-8")
      .getLines()
      .filterNot(line => line.startsWith("#") || line.isEmpty)
      .map(_.stripLineEnd.split(";", 6))
      .toList

    lines.map(line => Iso15924Def(line(0), line(1).toInt, line(2), line(3), Option(line(4)), line(5)))
  }
}
