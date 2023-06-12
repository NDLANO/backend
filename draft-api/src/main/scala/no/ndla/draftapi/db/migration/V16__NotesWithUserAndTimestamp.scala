/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migration

import enumeratum.Json4s
import no.ndla.common.model.domain.draft.DraftStatus
import no.ndla.common.model.domain.{EditorNote, Status}
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.ext.JavaTimeSerializers
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.{Extraction, Formats}
import org.postgresql.util.PGobject
import scalikejdbc._

import java.time.LocalDateTime
import scala.util.{Success, Try}

class V16__NotesWithUserAndTimestamp extends BaseJavaMigration {
  implicit val formats: Formats =
    org.json4s.DefaultFormats + Json4s.serializer(DraftStatus) ++ JavaTimeSerializers.all

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      migrateArticleNotes
    }

  def migrateArticleNotes(implicit session: DBSession): Unit = {
    val count        = countAllArticles.get
    var numPagesLeft = (count / 1000) + 1
    var offset       = 0L

    while (numPagesLeft > 0) {
      allArticles(offset * 1000).foreach { case (id, document) =>
        updateArticle(convertNotes(document), id)
      }
      numPagesLeft -= 1
      offset += 1
    }
  }

  def countAllArticles(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from articledata where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
  }

  def allArticles(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document from articledata where document is not null order by id limit 1000 offset $offset"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  def convertNotes(document: String): String = {
    val oldArticle = parse(document)
    Try(oldArticle.extract[V15__Article]) match {
      case Success(old) =>
        val newArticle = oldArticle.mapField {
          case ("notes", _) =>
            val editorNotes        = old.notes.map(EditorNote(_, "System", old.status, old.updated))
            val notesWithNewFormat = Extraction.decompose(editorNotes)
            ("notes", notesWithNewFormat)
          case x => x
        }
        compact(render(newArticle))
      case _ => compact(render(oldArticle))
    }
  }

  private def updateArticle(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update articledata set document = $dataObject where id = $id"
      .update()
  }

  case class V15__Article(status: Status, updated: LocalDateTime, notes: Seq[String])
}
