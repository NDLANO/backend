/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.JsonAST.{JField, JString}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.*
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V11__AddCreatedField extends BaseJavaMigration {
  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      allAudios.map { case (id: Long, document: String) =>
        update(convertDocument(document), id)
      }
    }: Unit

  def allAudios(implicit session: DBSession): List[(Long, String)] = {
    sql"select id, document from audiodata"
      .map(rs => (rs.long("id"), rs.string("document")))
      .list()
  }

  def convertDocument(document: String): String = {
    implicit val formats = DefaultFormats

    val oldArticle = parse(document)

    val updated       = (oldArticle \ "updated").extract[String]
    val objectToMerge = JObject(JField("created", JString(updated)))
    val newArticle    = oldArticle.merge(objectToMerge)
    compact(render(newArticle))
  }

  def update(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update audiodata set document = ${dataObject} where id = $id".update()
  }

}
