/*
 * Part of NDLA backend
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.language.model

import CodeLists.{Iso3166, iso3166Definitions}

import scala.util.{Failure, Success, Try}

object Iso3166 {

  def get(code: String): Try[Iso3166] = {
    iso3166Definitions.find(_.code.equalsIgnoreCase(code)) match {
      case Some(x) => Success(x)
      case None    => Failure(new RegionSubtagNotSupportedException(code))
    }
  }
}
