/*
 * Part of GDL language.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.language.model

import io.digitallibrary.language.model.CodeLists.{Iso3166, iso3166Definitions}

import scala.util.{Failure, Success, Try}

object Iso3166 {
  def get(code: String): Try[Iso3166] = {
    iso3166Definitions.find(_.code.equalsIgnoreCase(code)) match {
      case Some(x) => Success(x)
      case None => Failure(new RegionSubtagNotSupportedException(code))
    }
  }
}
