/*
 * Part of NDLA common.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain

import io.circe.{Decoder, Encoder}

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

  implicit val encoder: Encoder[Availability.Value] = Encoder.encodeEnumeration(Availability)
  implicit val decoder: Decoder[Availability.Value] = Decoder.decodeEnumeration(Availability)

}
