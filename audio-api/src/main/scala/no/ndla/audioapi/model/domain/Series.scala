/*
 * Part of NDLA audio-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.CirceUtil
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.Title
import no.ndla.language.Language.getSupportedLanguages
import scalikejdbc.*

import scala.util.Try

/** Base series without database generated fields */
class SeriesWithoutId(
    val title: Seq[Title],
    val coverPhoto: CoverPhoto,
    val episodes: Option[Seq[AudioMetaInformation]],
    val updated: NDLADate,
    val created: NDLADate,
    val description: Seq[Description],
    val hasRSS: Boolean
)
object SeriesWithoutId {
  implicit val encoder: Encoder[SeriesWithoutId] = deriveEncoder
  implicit val decoder: Decoder[SeriesWithoutId] = deriveDecoder
}

/** Series with database generated fields. Should match [[SeriesWithoutId]] exactly except for the fields added when
  * inserting into database.
  */
case class Series(
    id: Long,
    revision: Int,
    override val episodes: Option[Seq[AudioMetaInformation]],
    override val title: Seq[Title],
    override val coverPhoto: CoverPhoto,
    override val updated: NDLADate,
    override val created: NDLADate,
    override val description: Seq[Description],
    override val hasRSS: Boolean
) extends SeriesWithoutId(title, coverPhoto, episodes, updated, created, description, hasRSS) {
  lazy val supportedLanguages: Seq[String] = getSupportedLanguages(title, description)
}

object Series extends SQLSyntaxSupport[Series] {
  override val tableName = "seriesdata"

  implicit val encoder: Encoder[Series] = deriveEncoder
  implicit val decoder: Decoder[Series] = deriveDecoder

  def fromId(id: Long, revision: Int, series: SeriesWithoutId): Series = {
    new Series(
      id = id,
      revision = revision,
      episodes = None,
      title = series.title,
      coverPhoto = series.coverPhoto,
      updated = series.updated,
      created = series.created,
      description = series.description,
      hasRSS = series.hasRSS
    )
  }

  def fromResultSet(s: SyntaxProvider[Series])(rs: WrappedResultSet): Try[Series] =
    fromResultSet(s.resultName)(rs)

  def fromResultSet(s: ResultName[Series])(rs: WrappedResultSet): Try[Series] = {
    val jsonStr = rs.string(s.c("document"))
    val meta    = CirceUtil.tryParseAs[SeriesWithoutId](jsonStr)

    meta.map(m =>
      fromId(
        id = rs.long(s.c("id")),
        revision = rs.int(s.c("revision")),
        series = m
      )
    )
  }
}
