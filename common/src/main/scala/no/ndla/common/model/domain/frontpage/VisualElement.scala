/*
 * Part of NDLA common
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.frontpage

import enumeratum.*
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.errors.ValidationException

import scala.util.{Failure, Success, Try}

case class VisualElement(`type`: VisualElementType, id: String, alt: Option[String])

object VisualElement {
  implicit val encoder: Encoder[VisualElement] = deriveEncoder
  implicit val decoder: Decoder[VisualElement] = deriveDecoder
}

sealed abstract class VisualElementType(override val entryName: String) extends EnumEntry

object VisualElementType extends Enum[VisualElementType] with CirceEnum[VisualElementType] {
  case object Image      extends VisualElementType("image")
  case object Brightcove extends VisualElementType("brightcove")

  val values: IndexedSeq[VisualElementType] = findValues

  val all: Seq[String] = values.map(_.entryName)

  def validateVisualElement(visualElement: VisualElement): Try[VisualElement] = visualElement.`type` match {
    case Image => visualElement.id.toLongOption match {
        case None => Failure(ValidationException("visualElement.id", "Image of visual element should be numeric"))
        case _    => Success(visualElement)
      }
    case Brightcove => Success(visualElement)
  }

  def fromString(str: String): Try[VisualElementType] = VisualElementType.values.find(_.entryName == str) match {
    case Some(v) => Success(v)
    case None    => Failure(ValidationException("visualElement.type", s"'$str' is an invalid visual element type"))
  }

}
