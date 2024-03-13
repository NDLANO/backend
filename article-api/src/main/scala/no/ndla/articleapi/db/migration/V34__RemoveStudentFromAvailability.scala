/*
 * Part of NDLA article-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s._
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V34__RemoveStudentFromAvailability extends BaseJavaMigration {
  implicit val formats: Formats = DefaultFormats

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      migrateArticles
    }

  def migrateArticles(implicit session: DBSession): Unit = {
    val count        = countAllArticles.get
    var numPagesLeft = (count / 1000) + 1
    var offset       = 0L

    while (numPagesLeft > 0) {
      allArticles(offset * 1000).map { case (id, document) =>
        updateArticle(convertArticleUpdate(document), id)
      }: Unit
      numPagesLeft -= 1
      offset += 1
    }
  }

  def countAllArticles(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from contentdata where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
  }

  def allArticles(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document, article_id from contentdata where document is not null order by id limit 1000 offset $offset"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  def updateArticle(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update contentdata set document = $dataObject where id = $id"
      .update()
  }

  private[migration] def convertArticleUpdate(document: String): String = {
    val oldArticle = parse(document)

    val updated = oldArticle.mapField {
      case ("availability", JString("teacher")) => ("availability", Extraction.decompose("teacher"))
      case ("availability", _)                  => ("availability", Extraction.decompose("everyone"))
      case x                                    => x
    }

    compact(render(updated))
  }
}
