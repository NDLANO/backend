/*
 * Part of NDLA audio_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package db.migration

import java.sql.Connection
import java.util.Date

import no.ndla.audioapi.model.Language
import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.json4s.native.Serialization.{read, write}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V4__AddLanguageToAll extends JdbcMigration {
  implicit val formats = org.json4s.DefaultFormats

  override def migrate(connection: Connection): Unit = {
    val db = DB(connection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allAudios.map(convertAudioUpdate).foreach(update)
    }
  }

  def convertAudioUpdate(audioMeta: V4_AudioMetaInformation): V4_AudioMetaInformation = {
    audioMeta.copy(
      titles = audioMeta.titles.map(t => V4_Title(t.title, Some(Language.languageOrUnknown(t.language)))),
      filePaths = audioMeta.filePaths.map(f => V4_Audio(f.filePath, f.mimeType, f.fileSize, Some(Language.languageOrUnknown(f.language)))),
      tags = audioMeta.tags.map(t => V4_Tag(t.tags, Some(Language.languageOrUnknown(t.language))))
    )
  }

  def allAudios(implicit session: DBSession): List[V4_AudioMetaInformation] = {
    sql"select id, revision, document from audiodata".map(rs => {
      val meta = read[V4_AudioMetaInformation](rs.string("document"))
      V4_AudioMetaInformation(
        Some(rs.long("id")),
        Some(rs.int("revision")),
        meta.titles,
        meta.filePaths,
        meta.copyright,
        meta.tags,
        meta.updatedBy,
        meta.updated)
    }
    ).list().apply()
  }

  def update(audioMeta: V4_AudioMetaInformation)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(write(audioMeta))

    sql"update audiodata set document = $dataObject where id = ${audioMeta.id}".update().apply
  }

}

case class V4_AudioMetaInformation(id: Option[Long],
                                revision: Option[Int],
                                titles: Seq[V4_Title],
                                filePaths: Seq[V4_Audio],
                                copyright: V4_Copyright,
                                tags: Seq[V4_Tag],
                                updatedBy :String,
                                updated :Date)

case class V4_Title(title: String, language: Option[String])
case class V4_Audio(filePath: String, mimeType: String, fileSize: Long, language: Option[String])
case class V4_Copyright(license: String, origin: Option[String], authors: Seq[V4_Author])
case class V4_Author(`type`: String, name: String)
case class V4_Tag(tags: Seq[String], language: Option[String])