/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.model.domain

import no.ndla.frontpageapi.model.domain.Errors.ValidationException

import scala.util.{Failure, Success, Try}
import enumeratum._

case class VisualElement(`type`: VisualElementType, id: String, alt: Option[String])

sealed abstract class VisualElementType(override val entryName: String) extends EnumEntry

object VisualElementType extends Enum[VisualElementType] with CirceEnum[VisualElementType] {
  case object Image      extends VisualElementType("image")
  case object Brightcove extends VisualElementType("brightcove")

  val values: IndexedSeq[VisualElementType] = findValues

  val all: Seq[String] = values.map(_.entryName)

  def validateVisualElement(visualElement: VisualElement): Try[VisualElement] =
    visualElement.`type` match {
      case Image =>
        visualElement.id.toLongOption match {
          case None => Failure(ValidationException("Image of visual element should be numeric"))
          case _    => Success(visualElement)
        }
      case Brightcove => Success(visualElement)
    }

  def fromString(str: String): Try[VisualElementType] =
    VisualElementType.values.find(_.entryName == str) match {
      case Some(v) => Success(v)
      case None    => Failure(ValidationException(s"'$str' is an invalid visual element type"))
    }

}
