/*
 * Part of NDLA draft-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.ext.JavaTimeSerializers
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.{DefaultFormats, Extraction, Formats, JObject}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

import java.time.{LocalDateTime, ZoneOffset}

class V35__AddRevisionMeta extends BaseJavaMigration {
  implicit val formats: Formats = DefaultFormats ++ JavaTimeSerializers.all

  private val revisionInstant = LocalDateTime.now(ZoneOffset.UTC).plusYears(3)

  override def migrate(context: Context): Unit = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      migrateArticles
    }
  }

  def migrateArticles(implicit session: DBSession): Unit = {
    val count        = countAllArticles.get
    var numPagesLeft = (count / 1000) + 1
    var offset       = 0L

    while (numPagesLeft > 0) {
      allArticles(offset * 1000).map { case (id, document) =>
        updateArticle(convertArticleUpdate(document), id)
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
    sql"select id, document, article_id from articledata where document is not null order by id limit 1000 offset $offset"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  def updateArticle(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update articledata set document = $dataObject where id = $id"
      .update()
  }

  private[migration] def convertArticleUpdate(document: String): String = {
    val oldArticle  = parse(document)
    val toMergeWith = JObject("revisionMeta" -> Extraction.decompose(V35__RevisionMeta(revisionInstant, Seq.empty)))
    compact(render(oldArticle.merge(toMergeWith)))
  }

  case class V35__RevisionMeta(revisionDate: LocalDateTime, notes: Seq[String])
}
