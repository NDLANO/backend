/*
 * Part of NDLA audio-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.domain

import no.ndla.audioapi.AudioApiProperties
import no.ndla.audioapi.model.Language
import org.joda.time.DateTime
import org.json4s.FieldSerializer.ignore
import org.json4s.{DefaultFormats, FieldSerializer, Formats}
import org.json4s.native.Serialization
import scalikejdbc._

import scala.util.Try

/** Base series without database generated fields */
class SeriesWithoutId(
    val title: Seq[Title],
    val coverPhoto: CoverPhoto,
    val episodes: Option[Seq[AudioMetaInformation]],
    val updated: DateTime,
    val created: DateTime,
    val description: Seq[Description]
)

/** Series with database generated fields. Should match [[SeriesWithoutId]]
  * exactly except for the fields added when inserting into database. */
case class Series(
    id: Long,
    revision: Int,
    override val episodes: Option[Seq[AudioMetaInformation]],
    override val title: Seq[Title],
    override val coverPhoto: CoverPhoto,
    override val updated: DateTime,
    override val created: DateTime,
    override val description: Seq[Description]
) extends SeriesWithoutId(title, coverPhoto, episodes, updated, created, description) {
  lazy val supportedLanguages: Seq[String] = Language.getSupportedLanguages(title, description)
}

object Series extends SQLSyntaxSupport[Series] {
  val jsonEncoder: Formats = DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all

  val repositorySerializer: Formats = jsonEncoder +
    FieldSerializer[Series](
      ignore("id") orElse
        ignore("revision") orElse
        ignore("episodes")
    )

  override val tableName = "seriesdata"
  override val schemaName: Option[String] = Some(AudioApiProperties.MetaSchema)

  def fromId(id: Long, revision: Int, series: SeriesWithoutId): Series = {
    Series(
      id = id,
      revision = revision,
      episodes = None,
      title = series.title,
      coverPhoto = series.coverPhoto,
      updated = series.updated,
      created = series.created,
      description = series.description
    )
  }

  def fromResultSet(s: SyntaxProvider[Series])(rs: WrappedResultSet): Try[Series] =
    fromResultSet(s.resultName)(rs)

  def fromResultSet(s: ResultName[Series])(rs: WrappedResultSet): Try[Series] = {
    implicit val formats: Formats = jsonEncoder
    val jsonStr = rs.string(s.c("document"))
    val meta = Try(Serialization.read[SeriesWithoutId](jsonStr))

    meta.map(
      m =>
        fromId(
          id = rs.long(s.c("id")),
          revision = rs.int(s.c("revision")),
          series = m
      ))
  }
}
