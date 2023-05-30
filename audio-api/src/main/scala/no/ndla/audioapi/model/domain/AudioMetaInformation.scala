/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.domain

import io.circe.{Decoder, Encoder}
import no.ndla.audioapi.Props
import no.ndla.common.model.domain.{Author, Tag, Title}
import no.ndla.language.Language.getSupportedLanguages
import no.ndla.language.model.LanguageField
import org.json4s.FieldSerializer._
import org.json4s.ext.{EnumNameSerializer, JavaTimeSerializers}
import org.json4s.native.Serialization._
import org.json4s.{DefaultFormats, FieldSerializer, Formats}
import scalikejdbc._

import java.time.LocalDateTime

case class AudioMetaInformation(
    id: Option[Long],
    revision: Option[Int],
    titles: Seq[Title],
    filePaths: Seq[Audio],
    copyright: Copyright,
    tags: Seq[Tag],
    updatedBy: String,
    updated: LocalDateTime,
    created: LocalDateTime,
    podcastMeta: Seq[PodcastMeta],
    audioType: AudioType.Value = AudioType.Standard,
    manuscript: Seq[Manuscript],
    seriesId: Option[Long],
    series: Option[Series]
) {
  lazy val supportedLanguages: Seq[String] =
    getSupportedLanguages(titles, podcastMeta, manuscript, filePaths, tags)
}

object AudioType extends Enumeration {
  val Standard: this.Value = Value("standard")
  val Podcast: this.Value  = Value("podcast")

  def all: Seq[String]                       = this.values.map(_.toString).toSeq
  def valueOf(s: String): Option[this.Value] = this.values.find(_.toString == s)

  implicit val audioTypeEnc = Encoder.encodeEnumeration(AudioType)
  implicit val audioTypeDec = Decoder.decodeEnumeration(AudioType)
}

case class Manuscript(manuscript: String, language: String) extends LanguageField[String] {
  override def value: String    = manuscript
  override def isEmpty: Boolean = manuscript.isEmpty
}
case class Audio(filePath: String, mimeType: String, fileSize: Long, language: String) extends LanguageField[Audio] {
  override def value: Audio     = this
  override def isEmpty: Boolean = false
}
case class Copyright(
    license: String,
    origin: Option[String],
    creators: Seq[Author],
    processors: Seq[Author],
    rightsholders: Seq[Author],
    agreementId: Option[Long],
    validFrom: Option[LocalDateTime],
    validTo: Option[LocalDateTime]
)
trait DBAudioMetaInformation {
  this: Props =>

  object AudioMetaInformation extends SQLSyntaxSupport[AudioMetaInformation] {
    override val tableName                  = "audiodata"
    override val schemaName: Option[String] = Some(props.MetaSchema)

    val jsonEncoder: Formats = DefaultFormats + new EnumNameSerializer(AudioType) ++ JavaTimeSerializers.all

    val repositorySerializer: Formats = jsonEncoder +
      FieldSerializer[AudioMetaInformation](
        ignore("id") orElse
          ignore("revision") orElse
          ignore("external_id") orElse
          ignore("seriesId") orElse
          ignore("series")
      )

    def fromResultSet(au: SyntaxProvider[AudioMetaInformation])(rs: WrappedResultSet): AudioMetaInformation =
      fromResultSet(au.resultName)(rs)

    def fromResultSet(au: ResultName[AudioMetaInformation])(rs: WrappedResultSet): AudioMetaInformation = {
      implicit val formats: Formats = jsonEncoder
      val meta                      = read[AudioMetaInformation](rs.string(au.c("document")))
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

}

case class ReindexResult(totalIndexed: Int, millisUsed: Long)
