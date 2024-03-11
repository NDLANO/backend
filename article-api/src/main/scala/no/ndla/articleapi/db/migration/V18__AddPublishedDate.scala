/*
 * Part of NDLA article-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.*
import org.json4s.JsonAST.{JField, JObject}
import org.json4s.ext.JavaTimeSerializers
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

import java.time.LocalDateTime
import scala.util.{Success, Try}

class V18__AddPublishedDate extends BaseJavaMigration {
  implicit val formats: Formats = DefaultFormats ++ JavaTimeSerializers.all

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      migrateArticles
    }: Unit

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
    sql"select id, document from contentdata where document is not null order by id limit 1000 offset $offset"
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

  def convertArticleUpdate(document: String): String = {
    val oldArticle = parse(document)

    Try(oldArticle.extract[V17__Article]) match {
      case Success(_) =>
        val published  = JObject(JField("published", oldArticle \ "updated"))
        val newArticle = oldArticle merge published
        compact(render(newArticle))
      case _ => compact(render(oldArticle))
    }
  }
}
case class V17__Article(updated: LocalDateTime)
