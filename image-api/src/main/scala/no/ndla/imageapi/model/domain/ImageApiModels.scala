/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.domain

import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.common.model.domain.Author
import no.ndla.language.model.LanguageField

import java.time.LocalDateTime
import scala.util.{Failure, Success, Try}

case class ImageTitle(title: String, language: String) extends LanguageField[String] {
  override def value: String    = title
  override def isEmpty: Boolean = title.isEmpty
}
case class ImageAltText(alttext: String, language: String) extends LanguageField[String] {
  override def value: String    = alttext
  override def isEmpty: Boolean = alttext.isEmpty
}
case class ImageUrl(url: String, language: String) extends LanguageField[String] {
  override def value: String    = url
  override def isEmpty: Boolean = url.isEmpty
}
case class ImageCaption(caption: String, language: String) extends LanguageField[String] {
  override def value: String    = caption
  override def isEmpty: Boolean = caption.isEmpty
}
case class UploadedImage(
    fileName: String,
    size: Long,
    contentType: String,
    dimensions: Option[ImageDimensions]
)
case class Copyright(
    license: String,
    origin: String,
    creators: Seq[Author],
    processors: Seq[Author],
    rightsholders: Seq[Author],
    agreementId: Option[Long],
    validFrom: Option[LocalDateTime],
    validTo: Option[LocalDateTime]
)
case class License(license: String, description: String, url: Option[String])
case class EditorNote(timeStamp: LocalDateTime, updatedBy: String, note: String)
case class ImageDimensions(width: Int, height: Int)

object ModelReleasedStatus extends Enumeration {
  val YES            = Value("yes")
  val NO             = Value("no")
  val NOT_APPLICABLE = Value("not-applicable")
  val NOT_SET        = Value("not-set")

  def valueOfOrError(s: String): Try[this.Value] =
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

  def valueOf(s: String): Option[this.Value] = values.find(_.toString == s)
}

case class ReindexResult(totalIndexed: Int, millisUsed: Long)
