/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.{DefaultFormats, Extraction}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V15__ConvertLanguageUnknown extends BaseJavaMigration {
  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats

  override def migrate(context: Context): Unit =
    DB(context.getConnection)
      .autoClose(false)
      .withinTx { implicit session =>
        allAudios.map { case (id: Long, document: String) =>
          update(convertDocument(document), id)
        }
      }: Unit

  def convertDocument(document: String): String = {
    val oldAudio       = parse(document)
    val extractedAudio = oldAudio.extract[V15_Audio]
    val tags = extractedAudio.tags.map(t => {
      if (t.language == "unknown")
        t.copy(language = "und")
      else
        t
    })
    val titles = extractedAudio.titles.map(t => {
      if (t.language == "unknown")
        t.copy(language = "und")
      else
        t
    })
    val filePaths = extractedAudio.filePaths.map(fp => {
      if (fp.language == "unknown")
        fp.copy(language = "und")
      else
        fp
    })
    val manuscript = extractedAudio.manuscript.map(m => {
      if (m.language == "unknown")
        m.copy(language = "und")
      else
        m
    })
    val updated = oldAudio
      .replace(List("tags"), Extraction.decompose(tags))
      .replace(List("titles"), Extraction.decompose(titles))
      .replace(List("filePaths"), Extraction.decompose(filePaths))
      .replace(List("manuscript"), Extraction.decompose(manuscript))
    compact(render(updated))
  }

  def allAudios(implicit session: DBSession): List[(Long, String)] = {
    sql"select id, document from audiodata"
      .map(rs => (rs.long("id"), rs.string("document")))
      .list()
  }

  def update(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update audiodata set document = ${dataObject} where id = $id".update()
  }

  case class V15_Tags(tags: Seq[String], language: String)
  case class V15_Titles(title: String, language: String)
  case class V15_FilePaths(filePath: String, fileSize: Int, language: String, mimeType: String)
  case class V15_Manuscript(manuscript: String, language: String)
  case class V15_Audio(
      tags: Seq[V15_Tags],
      titles: Seq[V15_Titles],
      filePaths: Seq[V15_FilePaths],
      manuscript: Seq[V15_Manuscript]
  )
}
