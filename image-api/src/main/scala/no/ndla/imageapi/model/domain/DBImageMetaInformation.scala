/*
 * Part of NDLA image-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.domain

import no.ndla.imageapi.Props
import org.json4s.FieldSerializer.ignore
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.Serialization
import org.json4s.{DefaultFormats, FieldSerializer, Formats}

import java.util.Date
import scalikejdbc._

import scala.util.Try

case class ImageMetaInformation(
    id: Option[Long],
    titles: Seq[ImageTitle],
    alttexts: Seq[ImageAltText],
    images: Seq[ImageFileData],
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
  this: Props with DBImageFile =>

  object ImageMetaInformation extends SQLSyntaxSupport[ImageMetaInformation] {
    override val tableName: String          = "imagemetadata"
    override val schemaName: Option[String] = Some(props.MetaSchema)
    val jsonEncoder                         = new EnumNameSerializer(ModelReleasedStatus)
    val jsonEncoderWithDefaults: Formats    = DefaultFormats + jsonEncoder
    val repositorySerializer: Formats = {
      jsonEncoderWithDefaults +
        FieldSerializer[ImageMetaInformation](
          PartialFunction.empty
            .orElse(ignore("id"))
            .orElse(ignore("images"))
        )
    }

    def fromResultSet(im: SyntaxProvider[ImageMetaInformation])(rs: WrappedResultSet): ImageMetaInformation =
      fromResultSet(im.resultName)(rs)

    def fromResultSet(im: ResultName[ImageMetaInformation])(rs: WrappedResultSet): ImageMetaInformation = {
      implicit val formats: Formats = this.jsonEncoderWithDefaults
      val id                        = rs.long(im.c("id"))
      val jsonString                = rs.string(im.c("metadata"))
      val metaT                     = Try(Serialization.read[ImageMetaInformation](jsonString))
      val meta                      = metaT.get

      new ImageMetaInformation(
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
