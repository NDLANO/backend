/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.domain

import no.ndla.audioapi.AudioApiProperties
import org.json4s.FieldSerializer
import org.json4s.FieldSerializer._
import org.json4s.native.Serialization._
import scalikejdbc._

case class AudioMetaInformation(id: Option[Long], titles: Seq[AudioTitle], filePaths: Seq[AudioFilePath], audioInfo: Seq[AudioInfo])
case class AudioTitle(title: String, language: Option[String])
case class AudioFilePath(filePath: String, language: Option[String])
case class AudioInfo(mimeType: String, fileSize: Long, language: Option[String])

object AudioMetaInformation extends SQLSyntaxSupport[AudioMetaInformation] {
  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "audiometadata"
  override val schemaName = Some(AudioApiProperties.MetaSchema)

  def apply(im: SyntaxProvider[AudioMetaInformation])(rs:WrappedResultSet): AudioMetaInformation = apply(im.resultName)(rs)
  def apply(im: ResultName[AudioMetaInformation])(rs: WrappedResultSet): AudioMetaInformation = {
    val meta = read[AudioMetaInformation](rs.string(im.c("metadata")))
    AudioMetaInformation(None, meta.titles, meta.filePaths, meta.audioInfo)
  }

  val JSonSerializer = FieldSerializer[AudioMetaInformation](ignore("id"))
}
