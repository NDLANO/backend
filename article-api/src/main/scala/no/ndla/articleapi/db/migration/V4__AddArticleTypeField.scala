/*
 * Part of NDLA article-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.db.migration

import no.ndla.common.model.domain.ArticleType
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s._
import org.json4s.JsonAST.JString
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.postgresql.util.PGobject
import scalikejdbc._

class V4__AddUpdatedColoums extends BaseJavaMigration {

  implicit val formats: Formats = org.json4s.DefaultFormats

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      val count        = countAllArticles.get
      var numPagesLeft = (count / 1000) + 1
      var offset       = 0L

      while (numPagesLeft > 0) {
        allArticles(offset * 1000).map(convertArticleUpdate).foreach(update)
        numPagesLeft -= 1
        offset += 1
      }
    }

  def countAllArticles(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from contentdata where document is not NULL".map(rs => rs.long("count")).single()
  }

  def allArticles(offset: Long)(implicit session: DBSession): List[V4_DBArticleMetaInformation] = {
    sql"select id, document from contentdata where document is not NULL order by id limit 1000 offset ${offset}"
      .map(rs => V4_DBArticleMetaInformation(rs.long("id"), rs.string("document")))
      .list()
  }

  def convertArticleUpdate(articleMeta: V4_DBArticleMetaInformation): V4_DBArticleMetaInformation = {
    val oldDocument = parse(articleMeta.document)

    val updatedDocument = oldDocument mapField {
      case ("contentType", JString(cType)) =>
        val articleType =
          if (cType == "emneartikkel") ArticleType.TopicArticle.entryName else ArticleType.Standard.entryName
        ("articleType", JString(articleType))
      case x => x
    }

    articleMeta.copy(document = compact(render(updatedDocument)))
  }

  def update(articleMeta: V4_DBArticleMetaInformation)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(articleMeta.document)

    sql"update contentdata set document = $dataObject where id = ${articleMeta.id}".update()
  }

}

case class V4_DBArticleMetaInformation(id: Long, document: String)
