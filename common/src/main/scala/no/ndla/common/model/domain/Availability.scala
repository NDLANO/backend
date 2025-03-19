/*
 * Part of NDLA common
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain

import enumeratum.*

sealed trait Availability extends EnumEntry
object Availability extends Enum[Availability] with CirceEnum[Availability] {
  case object everyone extends Availability
  case object teacher  extends Availability

  def valueOf(s: String): Option[Availability] = {
    Availability.values.find(_.toString == s)
  }

  def valueOf(s: Option[String]): Option[Availability] = {
    s match {
      case None    => None
      case Some(s) => valueOf(s)
    }
  }

  def values: IndexedSeq[Availability] = findValues
}
