/*
 * Part of NDLA common.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain

object Availability extends Enumeration {
  val everyone, teacher = Value

  def valueOf(s: String): Option[Availability.Value] = {
    Availability.values.find(_.toString == s)
  }

  def valueOf(s: Option[String]): Option[Availability.Value] = {
    s match {
      case None    => None
      case Some(s) => valueOf(s)
    }
  }

}
