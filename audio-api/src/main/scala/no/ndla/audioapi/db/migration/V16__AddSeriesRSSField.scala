/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.JObject
import org.json4s.JsonAST.{JBool, JField}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V16__AddSeriesRSSField extends BaseJavaMigration {
  override def migrate(context: Context): Unit = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allAudios.map { case (id: Long, document: String) =>
        update(convertDocument(document), id)
      }
    }
  }

  def allAudios(implicit session: DBSession): List[(Long, String)] = {
    sql"select id, document from seriesdata"
      .map(rs => (rs.long("id"), rs.string("document")))
      .list()
  }

  def convertDocument(document: String): String = {
    val oldArticle = parse(document)

    val objectToMerge = JObject(
      JField("hasRSS", JBool(true))
    )

    val newArticle = oldArticle.merge(objectToMerge)
    compact(render(newArticle))
  }

  def update(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update seriesdata set document = ${dataObject} where id = $id".update()
  }

}
