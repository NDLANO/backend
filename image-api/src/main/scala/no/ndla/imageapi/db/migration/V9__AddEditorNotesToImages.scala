/*
 * Part of NDLA image-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.db.migration

import no.ndla.imageapi.model.domain.ModelReleasedStatus
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.JsonAST.{JField, JString}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.{DefaultFormats, JArray, JObject}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V9__AddEditorNotesToImages extends BaseJavaMigration {

  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
  val timeService                           = new TimeService()

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      imagesToUpdate.foreach { case (id, document) =>
        update(convertImageUpdate(document), id)
      }
    }

  def imagesToUpdate(implicit session: DBSession): List[(Long, String)] = {
    sql"select id, metadata from imagemetadata"
      .map(rs => {
        (rs.long("id"), rs.string("metadata"))
      })
      .list()
  }

  def convertImageUpdate(imageMeta: String): String = {
    val oldDocument     = parse(imageMeta)
    val updatedByString = (oldDocument \ "updatedBy").extract[String]
    val updatedString   = (oldDocument \ "updated").extract[String]

    val mergeObject = JObject(
      JField("createdBy", JString(updatedByString)),
      JField("created", JString(updatedString)),
      JField("modelReleased", JString(ModelReleasedStatus.NO.toString)),
      JField("editorNotes", JArray(List.empty))
    )

    val mergedDoc = oldDocument.merge(mergeObject)
    compact(render(mergedDoc))
  }

  def update(imagemetadata: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(imagemetadata)

    sql"update imagemetadata set metadata = ${dataObject} where id = $id".update()
  }
}
