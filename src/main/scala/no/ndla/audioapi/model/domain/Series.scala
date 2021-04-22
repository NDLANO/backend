package no.ndla.audioapi.model.domain

import no.ndla.audioapi.AudioApiProperties
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
)

/** Series with database generated fields. Should match [[SeriesWithoutId]]
  * exactly except for the fields added when inserting into database. */
case class Series(
    id: Long,
    revision: Int,
    override val episodes: Option[Seq[AudioMetaInformation]],
    override val title: Seq[Title],
    override val coverPhoto: CoverPhoto,
) extends SeriesWithoutId(title, coverPhoto, episodes)

object Series extends SQLSyntaxSupport[Series] {
  val jsonEncoder: Formats = DefaultFormats

  val repositorySerializer: Formats = jsonEncoder +
    FieldSerializer[Series](
      ignore("id") orElse
        ignore("revision")
    )

  override val tableName = "seriesdata"
  override val schemaName: Option[String] = Some(AudioApiProperties.MetaSchema)

  def fromId(id: Long, revision: Int, series: SeriesWithoutId): Series = {
    Series(
      id = id,
      revision = revision,
      title = series.title,
      coverPhoto = series.coverPhoto,
      episodes = None,
    )
  }

  def fromResultSet(s: SyntaxProvider[Series])(rs: WrappedResultSet): Try[Series] =
    fromResultSet(s.resultName)(rs)

  def fromResultSet(s: ResultName[Series])(rs: WrappedResultSet): Try[Series] = {
    implicit val formats: Formats = jsonEncoder
    val meta = Try(Serialization.read[SeriesWithoutId](rs.string(s.c("document"))))

    meta.map(
      m =>
        fromId(
          id = rs.long(s.c("id")),
          revision = rs.int(s.c("revision")),
          series = m
      ))
  }
}
