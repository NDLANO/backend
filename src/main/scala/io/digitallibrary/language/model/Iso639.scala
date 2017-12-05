/*
 * Part of GDL language.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.language.model

import io.digitallibrary.language.model.CodeLists.{Iso639, iso639Definitions}

import scala.util.{Failure, Success, Try}

object Iso639 {
  def get(code: String): Try[Iso639] = {
    val iso639 = code.length match {
      case 2 => iso639Definitions.find(_.part1.getOrElse("").equalsIgnoreCase(code))
      case 3 => findAlpha3(code)
      case _ => None
    }

    iso639 match {
      case Some(x) => Success(x)
      case None => Failure(new LanguageSubtagNotSupportedException(code))
    }
  }

  private def findAlpha3(code: String): Option[Iso639] = {
    val foundId: Option[Iso639] = iso639Definitions.find(iso => iso.id.equalsIgnoreCase(code))
    val found639_2t: Option[Iso639] = foundId match {
      case Some(x) => Some(x)
      case None => iso639Definitions.find(_.part2t.getOrElse("").equalsIgnoreCase(code))
    }

    found639_2t match {
      case Some(x) => Some(x)
      case None => iso639Definitions.find(_.part2b.getOrElse("").equalsIgnoreCase(code))
    }
  }
}


