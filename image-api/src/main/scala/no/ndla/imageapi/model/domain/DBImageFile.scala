/*
 * Part of NDLA image-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.domain

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.language.model.WithLanguage
import org.json4s.native.Serialization
import org.json4s.{DefaultFormats, Formats}
import scalikejdbc._

import scala.util.Try

case class ImageFileData(
    id: Long,
    fileName: String,
    size: Long,
    contentType: String,
    dimensions: Option[ImageDimensions],
    override val language: String,
    imageMetaId: Long
) extends WithLanguage {
  def toDocument(): ImageFileDataDocument = {
    ImageFileDataDocument(
      size = size,
      contentType = contentType,
      dimensions = dimensions,
      language = language
    )
  }
}

object ImageFileData {
  implicit val encoder: Encoder[ImageFileData] = deriveEncoder
  implicit val decoder: Decoder[ImageFileData] = deriveDecoder
}

case class ImageFileDataDocument(
    size: Long,
    contentType: String,
    dimensions: Option[ImageDimensions],
    override val language: String
) extends WithLanguage {
  def toFull(id: Long, fileName: String, imageId: Long): ImageFileData = {
    ImageFileData(
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

object Image extends SQLSyntaxSupport[ImageFileData] {
  override val tableName            = "imagefiledata"
  val jsonEncoder: Formats          = DefaultFormats
  val repositorySerializer: Formats = jsonEncoder

  def fromResultSet(im: SyntaxProvider[ImageFileData])(rs: WrappedResultSet): Try[Option[ImageFileData]] =
    fromResultSet(im.resultName)(rs)

  def fromResultSet(im: ResultName[ImageFileData])(rs: WrappedResultSet): Try[Option[ImageFileData]] = Try {
    implicit val formats: Formats = this.jsonEncoder
    for {
      id          <- rs.longOpt(im.c("id"))
      jsonString  <- rs.stringOpt(im.c("metadata"))
      fileName    <- rs.stringOpt(im.c("file_name"))
      imageMetaId <- rs.longOpt(im.c("image_meta_id"))
      document = Serialization.read[ImageFileDataDocument](jsonString)
    } yield document.toFull(id, fileName, imageMetaId)
  }
}
