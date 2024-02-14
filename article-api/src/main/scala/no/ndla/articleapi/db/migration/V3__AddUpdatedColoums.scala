/*
 * Part of NDLA article-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.Formats
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.postgresql.util.PGobject
import scalikejdbc._

class V3__AddUpdatedColoums extends BaseJavaMigration {

  implicit val formats: Formats = org.json4s.DefaultFormats

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      val count  = countAllArticles.get
      var delt   = (count / 1000) + 1
      var offset = 0L

      while (delt > 0) {
        allArticles(offset * 1000).map(convertArticleUpdate).foreach(update)
        delt -= 1
        offset += 1
      }
    }

  def countAllArticles(implicit session: DBSession) = {
    sql"select count(*) from contentdata where document is not NULL".map(rs => rs.long("count")).single()
  }

  def allArticles(offset: Long)(implicit session: DBSession) = {
    sql"select id, document from contentdata where document is not NULL order by id limit 1000 offset ${offset}"
      .map(rs => V3_DBArticleMetaInformation(rs.long("id"), rs.string("document")))
      .list()
  }

  def convertArticleUpdate(articleMeta: V3_DBArticleMetaInformation) = {
    val oldDocument = parse(articleMeta.document)
    val updatedJson = parse(s"""{"updatedBy": "content-import-client"}""")

    val mergedDoc = oldDocument merge updatedJson

    articleMeta.copy(document = compact(render(mergedDoc)))
  }

  def update(articleMeta: V3_DBArticleMetaInformation)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(articleMeta.document)

    sql"update contentdata set document = $dataObject where id = ${articleMeta.id}".update()
  }

}

case class V3_DBArticleMetaInformation(id: Long, document: String)
