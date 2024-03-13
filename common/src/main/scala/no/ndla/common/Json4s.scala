/*
 * Part of NDLA common.
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common

import enumeratum._
import org.json4s._

import scala.reflect.ClassTag

object Json4s {
  def serializer[A <: EnumEntry: ClassTag](e: Enum[A]): CustomSerializer[A] =
    new CustomSerializer[A](_ =>
      (
        { case JString(s) if e.withNameOption(s).isDefined => e.withName(s) },
        { case x: A => JString(x.entryName) }
      )
    )
}
