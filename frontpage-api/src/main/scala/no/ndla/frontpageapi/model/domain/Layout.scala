/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.domain
import enumeratum._
import no.ndla.frontpageapi.model.domain.Errors.ValidationException

import scala.util.{Failure, Success, Try}

case class Layout(`type`: LayoutType)

sealed trait LayoutType extends EnumEntry
case object LayoutType extends CirceEnum[LayoutType] with Enum[LayoutType] {

  case object Single  extends LayoutType
  case object Double  extends LayoutType
  case object Stacked extends LayoutType

  val values = findValues

  def fromString(string: String): Try[LayoutType] =
    LayoutType.values.find(_.toString == string) match {
      case Some(v) => Success(v)
      case None    => Failure(ValidationException(s"'$string' is an invalid layout"))
    }
}
