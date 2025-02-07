/*
 * Part of NDLA backend
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.language.model

import CodeLists.{Iso15924, iso15924Definitions}

import scala.util.{Failure, Success, Try}

object Iso15924 {

  def get(code: String): Try[Iso15924] = {
    iso15924Definitions.find(_.code.equalsIgnoreCase(code)) match {
      case Some(x) => Success(x)
      case None    => Failure(new ScriptSubtagNotSupportedException(code))
    }
  }
}
