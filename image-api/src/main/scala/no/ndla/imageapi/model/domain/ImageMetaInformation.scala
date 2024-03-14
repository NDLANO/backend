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
    editorNotes: Seq[EditorNote]
)

object ImageMetaInformation extends SQLSyntaxSupport[ImageMetaInformation] {
  override val tableName: String = "imagemetadata"

  implicit val encoder: Encoder[ImageMetaInformation] = deriveEncoder[ImageMetaInformation]
  implicit val decoder: Decoder[ImageMetaInformation] = deriveDecoder[ImageMetaInformation]

  def fromResultSet(im: SyntaxProvider[ImageMetaInformation])(rs: WrappedResultSet): ImageMetaInformation =
    fromResultSet(im.resultName)(rs)

  def fromResultSet(im: ResultName[ImageMetaInformation])(rs: WrappedResultSet): ImageMetaInformation = {
    val id         = rs.long(im.c("id"))
    val jsonString = rs.string(im.c("metadata"))
    val metaT      = CirceUtil.tryParseAs[ImageMetaInformation](jsonString)
    val meta       = metaT.get

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
