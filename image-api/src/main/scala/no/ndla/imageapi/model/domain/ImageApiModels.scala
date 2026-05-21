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
import no.ndla.common.model.NDLADate
import no.ndla.language.model.LanguageField
import sttp.tapir.{Codec, Schema}
import sttp.tapir.Codec.PlainCodec

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
    contentType: ImageContentType,
    dimensions: Option[ImageDimensions],
    variants: Seq[ImageVariant],
    originalDate: Option[String],
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

enum ModelReleasedStatus(val entryName: String) {
  case YES            extends ModelReleasedStatus("yes")
  case NO             extends ModelReleasedStatus("no")
  case NOT_APPLICABLE extends ModelReleasedStatus("not-applicable")
  case NOT_SET        extends ModelReleasedStatus("not-set")

  override def toString: String = entryName
}

object ModelReleasedStatus {
  def withNameOption(name: String): Option[ModelReleasedStatus] = values.find(_.entryName == name)

  implicit val codec: PlainCodec[ModelReleasedStatus] =
    Codec.derivedEnumeration[String, ModelReleasedStatus](decode = withNameOption, encode = _.entryName)

  implicit val encoder: Encoder[ModelReleasedStatus] = Encoder.encodeString.contramap(_.entryName)
  implicit val decoder: Decoder[ModelReleasedStatus] = Decoder
    .decodeString
    .emap(s => withNameOption(s).toRight(s"Unknown ModelReleasedStatus: $s"))
}
