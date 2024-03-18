/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.domain

import enumeratum._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.common.model.NDLADate
import no.ndla.language.model.LanguageField

import scala.util.{Failure, Success, Try}

case class ImageTitle(title: String, language: String) extends LanguageField[String] {
  override def value: String    = title
  override def isEmpty: Boolean = title.isEmpty
}

object ImageTitle {
  implicit val encoder: Encoder[ImageTitle] = deriveEncoder
  implicit val decoder: Decoder[ImageTitle] = deriveDecoder
}
case class ImageAltText(alttext: String, language: String) extends LanguageField[String] {
  override def value: String    = alttext
  override def isEmpty: Boolean = alttext.isEmpty
}

object ImageAltText {
  implicit val encoder: Encoder[ImageAltText] = deriveEncoder
  implicit val decoder: Decoder[ImageAltText] = deriveDecoder
}
case class ImageUrl(url: String, language: String) extends LanguageField[String] {
  override def value: String    = url
  override def isEmpty: Boolean = url.isEmpty
}

object ImageUrl {
  implicit val encoder: Encoder[ImageUrl] = deriveEncoder
  implicit val decoder: Decoder[ImageUrl] = deriveDecoder
}
case class ImageCaption(caption: String, language: String) extends LanguageField[String] {
  override def value: String    = caption
  override def isEmpty: Boolean = caption.isEmpty
}

object ImageCaption {
  implicit val encoder: Encoder[ImageCaption] = deriveEncoder
  implicit val decoder: Decoder[ImageCaption] = deriveDecoder
}
case class UploadedImage(
    fileName: String,
    size: Long,
    contentType: String,
    dimensions: Option[ImageDimensions]
)

object UploadedImage {
  implicit val encoder: Encoder[UploadedImage] = deriveEncoder
  implicit val decoder: Decoder[UploadedImage] = deriveDecoder
}
case class EditorNote(timeStamp: NDLADate, updatedBy: String, note: String)

object EditorNote {
  implicit val encoder: Encoder[EditorNote] = deriveEncoder
  implicit val decoder: Decoder[EditorNote] = deriveDecoder
}
case class ImageDimensions(width: Int, height: Int)
object ImageDimensions {
  implicit val encoder: Encoder[ImageDimensions] = deriveEncoder
  implicit val decoder: Decoder[ImageDimensions] = deriveDecoder
}

sealed abstract class ModelReleasedStatus(override val entryName: String) extends EnumEntry

object ModelReleasedStatus extends Enum[ModelReleasedStatus] with CirceEnum[ModelReleasedStatus] {
  case object YES            extends ModelReleasedStatus("yes")
  case object NO             extends ModelReleasedStatus("no")
  case object NOT_APPLICABLE extends ModelReleasedStatus("not-applicable")
  case object NOT_SET        extends ModelReleasedStatus("not-set")

  def valueOfOrError(s: String): Try[ModelReleasedStatus] =
    valueOf(s) match {
      case Some(st) => Success(st)
      case None =>
        val validStatuses = values.map(_.toString).mkString(", ")
        Failure(
          new ValidationException(
            errors = Seq(
              ValidationMessage(
                "modelReleased",
                s"'$s' is not a valid model released status. Must be one of $validStatuses"
              )
            )
          )
        )
    }

  def valueOf(s: String): Option[ModelReleasedStatus] = values.find(_.toString == s)

  override def values: IndexedSeq[ModelReleasedStatus] = findValues
}

case class ReindexResult(totalIndexed: Int, millisUsed: Long)
