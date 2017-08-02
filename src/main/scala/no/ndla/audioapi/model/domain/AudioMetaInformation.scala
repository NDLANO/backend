/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.domain

import java.util.Date

import io.searchbox.client.JestResult
import no.ndla.audioapi.AudioApiProperties
import no.ndla.audioapi.model.Language
import no.ndla.audioapi.model.Language.UnknownLanguage
import org.json4s.FieldSerializer
import org.json4s.FieldSerializer._
import org.json4s.native.Serialization._
import scalikejdbc._

case class AudioMetaInformation(id: Option[Long],
                                revision: Option[Int],
                                titles: Seq[Title],
                                filePaths: Seq[Audio],
                                copyright: Copyright,
                                tags: Seq[Tag],
                                updatedBy :String,
                                updated :Date) {
  lazy val supportedLanguages = titles.map(_.language.getOrElse(Language.UnknownLanguage))
    .union(tags.map(_.language.getOrElse(UnknownLanguage)))
    .distinct
}

case class Title(title: String, language: Option[String]) extends LanguageField[String] { override def value: String = title }
case class Audio(filePath: String, mimeType: String, fileSize: Long, language: Option[String]) extends LanguageField[Audio] { override def value: Audio = this }
case class Copyright(license: String, origin: Option[String], authors: Seq[Author])
case class Author(`type`: String, name: String)
case class Tag(tags: Seq[String], language: Option[String]) extends LanguageField[Seq[String]] { override def value: Seq[String] = tags }

object AudioMetaInformation extends SQLSyntaxSupport[AudioMetaInformation] {
  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "audiodata"
  override val schemaName = Some(AudioApiProperties.MetaSchema)

  def apply(au: SyntaxProvider[AudioMetaInformation])(rs:WrappedResultSet): AudioMetaInformation = apply(au.resultName)(rs)
  def apply(au: ResultName[AudioMetaInformation])(rs: WrappedResultSet): AudioMetaInformation = {
    val meta = read[AudioMetaInformation](rs.string(au.c("document")))
    AudioMetaInformation(
      Some(rs.long(au.c("id"))),
      Some(rs.int(au.c("revision"))),
      meta.titles,
      meta.filePaths,
      meta.copyright,
      meta.tags,
      meta.updatedBy,
      meta.updated)
  }

  val JSonSerializer = FieldSerializer[AudioMetaInformation](
    ignore("id") orElse
    ignore("revision") orElse
    ignore("external_id")
  )
}

class NdlaSearchException(jestResponse: JestResult) extends RuntimeException(jestResponse.getErrorMessage) {
  def getResponse: JestResult = jestResponse
}

case class ReindexResult(totalIndexed: Int, millisUsed: Long)