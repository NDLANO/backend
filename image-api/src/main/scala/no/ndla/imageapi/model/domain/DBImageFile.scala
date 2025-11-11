/*
 * Part of NDLA image-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.domain

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.CirceUtil
import no.ndla.language.model.WithLanguage
import scalikejdbc.*

import scala.util.Try

case class ImageFileData(
    id: Long,
    fileName: String,
    size: Long,
    contentType: String,
    dimensions: Option[ImageDimensions],
    variants: Seq[ImageVariant],
    override val language: String,
    imageMetaId: Long,
) extends WithLanguage {
  def toDocument(): ImageFileDataDocument = {
    ImageFileDataDocument(
      size = size,
      contentType = contentType,
      dimensions = dimensions,
      variants = variants,
      language = language,
    )
  }

  def getFileStem: String = {
    fileName.lastIndexOf(".") match {
      case i if i > 0 => fileName.substring(0, i)
      case _          => fileName
    }
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
    variants: Seq[ImageVariant],
    override val language: String,
) extends WithLanguage {
  def toFull(id: Long, fileName: String, imageId: Long): ImageFileData = {
    ImageFileData(
      id = id,
      fileName = fileName,
      size = size,
      contentType = contentType,
      dimensions = dimensions,
      variants = variants,
      language = language,
      imageMetaId = imageId,
    )
  }
}

object ImageFileDataDocument {
  implicit val encoder: Encoder[ImageFileDataDocument] = deriveEncoder
  implicit val decoder: Decoder[ImageFileDataDocument] = deriveDecoder
}

object Image extends SQLSyntaxSupport[ImageFileData] {
  override val tableName = "imagefiledata"

  def fromResultSet(im: SyntaxProvider[ImageFileData])(rs: WrappedResultSet): Try[Option[ImageFileData]] =
    fromResultSet(im.resultName)(rs)

  def fromResultSet(im: ResultName[ImageFileData])(rs: WrappedResultSet): Try[Option[ImageFileData]] = Try {
    for {
      id          <- rs.longOpt(im.c("id"))
      jsonString  <- rs.stringOpt(im.c("metadata"))
      fileName    <- rs.stringOpt(im.c("file_name"))
      imageMetaId <- rs.longOpt(im.c("image_meta_id"))
      document     = CirceUtil.tryParseAs[ImageFileDataDocument](jsonString).get
    } yield document.toFull(id, fileName, imageMetaId)
  }
}
