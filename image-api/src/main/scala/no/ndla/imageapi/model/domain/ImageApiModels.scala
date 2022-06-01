/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.domain

import java.util.Date
import no.ndla.imageapi.{ImageApiProperties, Props}
import no.ndla.imageapi.model.{ValidationException, ValidationMessage}
import no.ndla.language.model.{LanguageField, WithLanguage}
import org.json4s.{DefaultFormats, FieldSerializer, Formats}
import org.json4s.FieldSerializer._
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.Serialization._
import scalikejdbc._

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
case class ImageTag(tags: Seq[String], language: String) extends LanguageField[Seq[String]] {
  override def value: Seq[String] = tags
  override def isEmpty: Boolean   = tags.isEmpty
}
case class Image(
    fileName: String,
    size: Long,
    contentType: String,
    dimensions: Option[ImageDimensions],
    language: String
) extends WithLanguage
case class Copyright(
    license: String,
    origin: String,
    creators: Seq[Author],
    processors: Seq[Author],
    rightsholders: Seq[Author],
    agreementId: Option[Long],
    validFrom: Option[Date],
    validTo: Option[Date]
)
case class License(license: String, description: String, url: Option[String])
case class Author(`type`: String, name: String)
case class EditorNote(timeStamp: Date, updatedBy: String, note: String)
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

case class ImageMetaInformation(
    id: Option[Long],
    titles: Seq[ImageTitle],
    alttexts: Seq[ImageAltText],
    images: Seq[Image],
    copyright: Copyright,
    tags: Seq[ImageTag],
    captions: Seq[ImageCaption],
    updatedBy: String,
    updated: Date,
    created: Date,
    createdBy: String,
    modelReleased: ModelReleasedStatus.Value,
    editorNotes: Seq[EditorNote]
)

trait DBImageMetaInformation {
  this: Props =>
  object DBImageMetaInformation extends SQLSyntaxSupport[ImageMetaInformation] {
    override val tableName   = "imagemetadata"
    override val schemaName  = Some(props.MetaSchema)
    val jsonEncoder: Formats = DefaultFormats + new EnumNameSerializer(ModelReleasedStatus)
    val repositorySerializer = jsonEncoder + FieldSerializer[ImageMetaInformation](ignore("id"))

    def fromResultSet(im: SyntaxProvider[ImageMetaInformation])(rs: WrappedResultSet): ImageMetaInformation =
      fromResultSet(im.resultName)(rs)

    def fromResultSet(im: ResultName[ImageMetaInformation])(rs: WrappedResultSet): ImageMetaInformation = {
      implicit val formats: Formats = this.jsonEncoder
      val id                        = rs.long(im.c("id"))
      val jsonString                = rs.string(im.c("metadata"))
      val meta                      = read[ImageMetaInformation](jsonString)

      ImageMetaInformation(
        id = Some(id),
        titles = meta.titles,
        alttexts = meta.alttexts,
        images = meta.images,
        copyright = meta.copyright,
        tags = meta.tags,
        captions = meta.captions,
        updatedBy = meta.updatedBy,
        updated = meta.updated,
        created = meta.created,
        createdBy = meta.createdBy,
        modelReleased = meta.modelReleased,
        editorNotes = meta.editorNotes
      )
    }
  }
}

case class ReindexResult(totalIndexed: Int, millisUsed: Long)
