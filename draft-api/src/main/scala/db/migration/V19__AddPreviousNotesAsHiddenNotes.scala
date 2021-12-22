/*
 * Part of NDLA draft-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package db.migration

import java.util.Date

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.{DefaultFormats, Extraction}
import org.json4s.JsonAST.{JArray, JField, JObject}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.native.Serialization.read
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

import scala.util.{Success, Try}

class V19__AddPreviousNotesAsHiddenNotes extends BaseJavaMigration {
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
        case (id, document, article_id) => updateArticle(convertArticleUpdate(article_id, id, document), id)
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

  def allArticles(offset: Long)(implicit session: DBSession): Seq[(Long, String, Long)] = {
    sql"select id, document, article_id from articledata where document is not null order by id limit 1000 offset $offset"
      .map(rs => {
        (rs.long("id"), rs.string("document"), rs.long("article_id"))
      })
      .list()
  }

  def allArticlesWithArticleId(articleId: Long)(implicit session: DBSession) = {
    sql"select id, document from articledata where document is not null and article_id=${articleId} order by id"
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

  def convertArticleUpdate(articleId: Long, id: Long, document: String)(implicit session: DBSession): String = {
    val oldArticle = parse(document)

    val allVersions = allArticlesWithArticleId(articleId)
    val allPreviousVersions = allVersions.filter(_._1 < id)
    val allPreviousNotes = allPreviousVersions.flatMap(artTup => read[V18__Article](artTup._2).notes)

    Try(oldArticle.extract[V18__Article]) match {
      case Success(art) =>
        val previousNotes = JObject(JField("previousVersionsNotes", Extraction.decompose(allPreviousNotes)))
        val newArticle = oldArticle.merge(previousNotes)
        compact(render(newArticle))
      case _ => compact(render(oldArticle))
    }
  }
}
case class V18__Article(notes: Seq[V18__EditorNote])
case class V18__EditorNote(note: String, user: String, status: V18__Status, timestamp: Date)
case class V18__Status(current: String, other: Set[String])
