/*
 * Part of NDLA image-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.domain

import no.ndla.imageapi.Props
import no.ndla.language.model.WithLanguage
import org.json4s.native.Serialization
import org.json4s.{DefaultFormats, Formats}
import scalikejdbc._

import scala.util.Try

case class Image( // TODO: Rename to ImageFileMeta for clarity?
    id: Long,
    fileName: String,
    size: Long,
    contentType: String,
    dimensions: Option[ImageDimensions],
    override val language: String,
    imageMetaId: Long
) extends ImageDocument(size, contentType, dimensions, language)

class ImageDocument(
    size: Long,
    contentType: String,
    dimensions: Option[ImageDimensions],
    override val language: String
) extends WithLanguage {
  def toFull(id: Long, fileName: String, imageId: Long): Image = {
    new Image(
      id = id,
      fileName = fileName,
      size = size,
      contentType = contentType,
      dimensions = dimensions,
      language = language,
      imageMetaId = imageId
    )
  }
}

trait DBImageFile {
  this: Props =>

  object Image extends SQLSyntaxSupport[Image] {
    override val tableName                  = "imagefiledata"
    override val schemaName: Option[String] = Some(props.MetaSchema)
    val jsonEncoder: Formats                = DefaultFormats
    val repositorySerializer: Formats       = jsonEncoder

    def fromResultSet(im: SyntaxProvider[Image])(rs: WrappedResultSet): Try[Image] =
      fromResultSet(im.resultName)(rs)

    def fromResultSet(im: ResultName[Image])(rs: WrappedResultSet): Try[Image] = Try {
      implicit val formats: Formats = this.jsonEncoder
      val id                        = rs.long(im.c("id"))
      val jsonString                = rs.string(im.c("metadata"))
      val fileName                  = rs.string(im.c("file_name"))
      val imageMetaId               = rs.long(im.c("image_meta_id"))
      val documentMeta              = Serialization.read[ImageDocument](jsonString)
      documentMeta.toFull(id, fileName, imageMetaId)
    }
  }
}
