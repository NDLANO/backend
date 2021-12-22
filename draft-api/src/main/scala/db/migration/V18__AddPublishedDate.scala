/*
 * Part of NDLA draft-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package db.migration

import java.util.Date

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.DefaultFormats
import org.json4s.JsonAST.{JField, JObject}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

import scala.util.{Success, Try}

class V18__AddPublishedDate extends BaseJavaMigration {
  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats

  override def migrate(context: Context): Unit = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      migrateArticles
    }
  }

  def migrateArticles(implicit session: DBSession): Unit = {
    val count = countAllArticles.get
    var numPagesLeft = (count / 1000) + 1
    var offset = 0L

    while (numPagesLeft > 0) {
      allArticles(offset * 1000).map {
        case (id, document) => updateArticle(convertArticleUpdate(document), id)
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

  def updateArticle(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update articledata set document = $dataObject where id = $id"
      .update()
  }

  def convertArticleUpdate(document: String): String = {
    val oldArticle = parse(document)

    Try(oldArticle.extract[V17__Article]) match {
      case Success(_) =>
        val published = JObject(JField("published", oldArticle \ "updated"))
        val newArticle = oldArticle merge published
        compact(render(newArticle))
      case _ => compact(render(oldArticle))
    }
  }
}
case class V17__Article(updated: Date)
