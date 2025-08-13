/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.domain

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

object ModelReleasedStatus extends Enumeration {
  val YES: Value            = Value("yes")
  val NO: Value             = Value("no")
  val NOT_APPLICABLE: Value = Value("not-applicable")
  val NOT_SET: Value        = Value("not-set")

  def valueOfOrError(s: String): Try[this.Value] =
    valueOf(s) match {
      case Some(st) => Success(st)
      case None     =>
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

  def valueOf(s: String): Option[this.Value] = values.find(_.toString == s)

  implicit val encoder: Encoder[ModelReleasedStatus.Value] = Encoder.encodeEnumeration(ModelReleasedStatus)
  implicit val decoder: Decoder[ModelReleasedStatus.Value] = Decoder.decodeEnumeration(ModelReleasedStatus)
}
