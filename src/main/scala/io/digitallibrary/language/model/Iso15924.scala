/*
 * Part of GDL language.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.language.model

import io.digitallibrary.language.model.CodeLists.{Iso15924, iso15924Definitions}

import scala.util.{Failure, Success, Try}

object Iso15924 {
  def get(code: String): Try[Iso15924] = {
    iso15924Definitions.find(_.code.equalsIgnoreCase(code.toLowerCase)) match {
      case Some(x) => Success(x)
      case None => Failure(new ScriptSubtagNotSupportedException(code))
    }
  }
}