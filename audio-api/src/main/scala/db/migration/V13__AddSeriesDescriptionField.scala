/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.joda.time.DateTime
import org.json4s.JsonAST.JField
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.{DefaultFormats, Extraction, Formats, JObject}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V13__AddSeriesDescriptionField extends BaseJavaMigration {
  private implicit val formats: Formats = DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all

  override def migrate(context: Context): Unit = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allSeries.map {
        case (id: Long, document: String) => update(convertDocument(document), id)
      }
    }
  }

  def allSeries(implicit session: DBSession): List[(Long, String)] = {
    sql"select id, document from seriesdata"
      .map(rs => (rs.long("id"), rs.string("document")))
      .list()
  }

  def convertDocument(document: String): String = {
    val oldSeries = parse(document)

    val descriptions = (oldSeries \ "title").mapField {
      case "title" -> x => "description" -> x
      case x            => x
    }

    val objectToMerge = JObject(JField("description", descriptions))
    val newSeries = oldSeries.merge(objectToMerge)
    compact(render(newSeries))
  }

  def update(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update seriesdata set document = ${dataObject} where id = $id".update()
  }

}
