/*
 * Part of NDLA image-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.CirceUtil
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.Tag
import no.ndla.common.model.domain.article.Copyright
import no.ndla.imageapi.Props
import scalikejdbc.*

import scala.util.Try

case class ImageMetaInformation(
    id: Option[Long],
    titles: Seq[ImageTitle],
    alttexts: Seq[ImageAltText],
    images: Seq[ImageFileData],
    copyright: Copyright,
    tags: Seq[Tag],
    captions: Seq[ImageCaption],
    updatedBy: String,
    updated: NDLADate,
    created: NDLADate,
    createdBy: String,
    modelReleased: ModelReleasedStatus.Value,
    editorNotes: Seq[EditorNote],
    inactive: Boolean,
)

object ImageMetaInformation {

  implicit val encoder: Encoder[ImageMetaInformation] = deriveEncoder[ImageMetaInformation]
  implicit val decoder: Decoder[ImageMetaInformation] = deriveDecoder[ImageMetaInformation]

  def fromResultSet(im: SyntaxProvider[ImageMetaInformation])(rs: WrappedResultSet): Try[ImageMetaInformation] =
    fromResultSet(im.resultName)(rs)

  def fromResultSet(im: ResultName[ImageMetaInformation])(rs: WrappedResultSet): Try[ImageMetaInformation] = {
    val id         = rs.long(im.c("id"))
    val jsonString = rs.string(im.c("metadata"))
    CirceUtil.tryParseAs[ImageMetaInformation](jsonString).map(_.copy(Some(id)))
  }
}

class DBImageMetaInformation(using props: Props) extends SQLSyntaxSupport[ImageMetaInformation] {
  override val tableName: String          = "imagemetadata"
  override val schemaName: Option[String] = Some(props.MetaSchema)
}
