/*
 * Part of NDLA concept-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.DefaultFormats
import org.json4s.JsonAST.{JArray, JObject, JString}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V5__MigrateStatusToQualityAssured extends BaseJavaMigration {
  implicit val formats: DefaultFormats.type = DefaultFormats

  override def migrate(context: Context): Unit = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      migrateConcepts
    }
  }

  def migrateConcepts(implicit session: DBSession): Unit = {
    val count = countAllConcepts.get
    var numPagesLeft = (count / 1000) + 1
    var offset = 0L

    while (numPagesLeft > 0) {
      allConcepts(offset * 1000).map {
        case (id, document) => updateConcept(convertToNewConcept(document), id)
      }
      numPagesLeft -= 1
      offset += 1
    }
  }

  def countAllConcepts(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from conceptdata where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
  }

  def allConcepts(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document from conceptdata where document is not null order by id limit 1000 offset $offset"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  def updateConcept(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update conceptdata set document = $dataObject where id = $id"
      .update()
  }

  def renameStatus(status: String): String = {
    val mapping = Map(
      "QUEUED_FOR_PUBLISHING" -> "QUALITY_ASSURED"
    )
    mapping.getOrElse(status, status)
  }

  def convertToNewConcept(document: String): String = {
    val concept = parse(document)

    val newConcept = concept.mapField {
      case ("status", status: JObject) =>
        "status" -> status.mapField {
          case ("current", current: JString) =>
            "current" -> JString(renameStatus(current.values))
          case x => x
        }
      case x => x
    }
    compact(render(newConcept))
  }
}
