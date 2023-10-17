/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.db.migration

import io.circe.parser.parse
import io.circe.{Json, JsonObject}
import no.ndla.frontpageapi.repository._
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.postgresql.util.PGobject
import scalikejdbc._

import scala.util.{Failure, Success}

class V9__add_missing_fields extends BaseJavaMigration {
  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      subjectPageData
        .map(convertSubjectpage)
        .foreach(update)
    }

  private def subjectPageData(implicit session: DBSession): List[DBSubjectPage] = {
    sql"select id, document from subjectpage"
      .map(rs => DBSubjectPage(rs.long("id"), rs.string("document")))
      .list()
  }

  implicit class JsonObjectOps(obj: JsonObject) {
    def addIfNotExists(key: String, value: Json): JsonObject = {
      if (obj.contains(key)) obj
      else obj.add(key, value)
    }
  }

  def convertSubjectpage(subjectPageData: DBSubjectPage): DBSubjectPage =
    parse(subjectPageData.document).toTry match {
      case Success(value) =>
        val newSubjectPage = value.mapObject(obj => {
          obj
            .addIfNotExists("connectedTo", Json.arr())
            .addIfNotExists("buildsOn", Json.arr())
            .addIfNotExists("leadsTo", Json.arr())
            .remove("topical")
            .remove("mostRead")
            .remove("latestContent")
            .remove("filters")
            .remove("layout")
            .remove("twitter")
            .remove("facebook")
            .remove("goTo")
        })
        DBSubjectPage(subjectPageData.id, newSubjectPage.noSpacesDropNull)
      case Failure(ex) =>
        println(s"Failed to parse subject page data for id '${subjectPageData.id}': ${ex.getMessage}")
        throw ex
    }

  private def update(subjectPageData: DBSubjectPage)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(subjectPageData.document)

    sql"update subjectpage set document = $dataObject where id = ${subjectPageData.id}"
      .update()
  }
}
