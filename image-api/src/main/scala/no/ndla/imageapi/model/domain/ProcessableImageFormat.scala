package no.ndla.imageapi.model.domain

import com.sksamuel.scrimage.format.Format
import enumeratum.{Enum, EnumEntry}

sealed trait ProcessableImageFormat extends EnumEntry

object ProcessableImageFormat extends Enum[ProcessableImageFormat] {
  case object Jpeg extends ProcessableImageFormat
  case object Png  extends ProcessableImageFormat
  case object Webp extends ProcessableImageFormat

  def fromScrimageFormat(format: Format): Option[ProcessableImageFormat] = format match {
    case Format.PNG  => Some(Png)
    case Format.JPEG => Some(Jpeg)
    case Format.WEBP => Some(Webp)
    case Format.GIF  => None
  }

  override def values: IndexedSeq[ProcessableImageFormat] = findValues
}
