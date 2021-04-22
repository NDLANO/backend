/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.domain

import java.util.Date
import com.sksamuel.elastic4s.http.RequestFailure
import no.ndla.audioapi.AudioApiProperties
import no.ndla.audioapi.model.Language
import no.ndla.audioapi.model.Language.UnknownLanguage
import org.json4s.{DefaultFormats, FieldSerializer, Formats}
import org.json4s.FieldSerializer._
import org.json4s.ext.EnumSerializer
import org.json4s.native.Serialization._
import scalikejdbc._

case class AudioMetaInformation(
    id: Option[Long],
    revision: Option[Int],
    titles: Seq[Title],
    filePaths: Seq[Audio],
    copyright: Copyright,
    tags: Seq[Tag],
    updatedBy: String,
    updated: Date,
    podcastMeta: Seq[PodcastMeta],
    audioType: AudioType.Value = AudioType.Standard,
    manuscript: Seq[Manuscript],
    seriesId: Option[Long]
) {
  lazy val supportedLanguages: Seq[String] = Language.getSupportedLanguages(titles, filePaths, tags)
}

object AudioType extends Enumeration {
  val Standard: this.Value = Value("standard")
  val Podcast: this.Value = Value("podcast")

  def all: Seq[String] = this.values.map(_.toString).toSeq
  def valueOf(s: String): Option[this.Value] = this.values.find(_.toString == s)
}

case class Title(title: String, language: String) extends LanguageField[String] { override def value: String = title }
case class Manuscript(manuscript: String, language: String) extends LanguageField[String] {
  override def value: String = manuscript
}
case class Audio(filePath: String, mimeType: String, fileSize: Long, language: String) extends LanguageField[Audio] {
  override def value: Audio = this
}
case class Copyright(license: String,
                     origin: Option[String],
                     creators: Seq[Author],
                     processors: Seq[Author],
                     rightsholders: Seq[Author],
                     agreementId: Option[Long],
                     validFrom: Option[Date],
                     validTo: Option[Date])
case class Author(`type`: String, name: String)
case class Tag(tags: Seq[String], language: String) extends LanguageField[Seq[String]] {
  override def value: Seq[String] = tags
}

object AudioMetaInformation extends SQLSyntaxSupport[AudioMetaInformation] {
  override val tableName = "audiodata"
  override val schemaName: Option[String] = Some(AudioApiProperties.MetaSchema)

  val jsonEncoder: Formats = DefaultFormats + new EnumSerializer(AudioType)

  val repositorySerializer: Formats = jsonEncoder +
    FieldSerializer[AudioMetaInformation](
      ignore("id") orElse
        ignore("revision") orElse
        ignore("external_id")
    )

  def fromResultSet(au: SyntaxProvider[AudioMetaInformation])(rs: WrappedResultSet): AudioMetaInformation =
    fromResultSet(au.resultName)(rs)

  def fromResultSet(au: ResultName[AudioMetaInformation])(rs: WrappedResultSet): AudioMetaInformation = {
    implicit val formats: Formats = jsonEncoder
    val meta = read[AudioMetaInformation](rs.string(au.c("document")))
    meta.copy(
      id = Some(rs.long(au.c("id"))),
      revision = Some(rs.int(au.c("revision"))),
      seriesId = rs.longOpt(au.c("series_id"))
    )
  }

  def fromResultSetOpt(au: ResultName[AudioMetaInformation])(rs: WrappedResultSet): Option[AudioMetaInformation] = {
    rs.longOpt(au.c("id")).map(_ => fromResultSet(au)(rs))
  }
}

case class NdlaSearchException(rf: RequestFailure)
    extends RuntimeException(
      s"""
     |index: ${rf.error.index.getOrElse("Error did not contain index")}
     |reason: ${rf.error.reason}
     |body: ${rf.body}
     |shard: ${rf.error.shard.getOrElse("Error did not contain shard")}
     |type: ${rf.error.`type`}
   """.stripMargin
    )

case class ReindexResult(totalIndexed: Int, millisUsed: Long)
