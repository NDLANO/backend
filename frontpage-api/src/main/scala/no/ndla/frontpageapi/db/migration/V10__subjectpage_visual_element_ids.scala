/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.db.migration

import io.circe.parser.parse
import io.circe.{Json, JsonObject}
import no.ndla.frontpageapi.repository._
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.postgresql.util.PGobject
import scalikejdbc._

import scala.util.{Failure, Success}

class V10__subjectpage_visual_element_ids extends BaseJavaMigration {
  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      subjectPageData
        .map(convertSubjectpage)
        .foreach(update)
    }

  private def subjectPageData(implicit session: DBSession): List[V10__DBSubjectPage] = {
    sql"select id, document from subjectpage"
      .map(rs => V10__DBSubjectPage(rs.long("id"), rs.string("document")))
      .list()
  }

  implicit class JsonObjectOps(obj: JsonObject) {
    def addIfNotExists(key: String, value: Json): JsonObject = {
      if (obj.contains(key)) obj
      else obj.add(key, value)
    }
  }

  def replaceUrlWithId(maybeUrl: String): String = {
    maybeUrl.replaceAll("https://(.*)/image-api/raw/id/", "")
  }

  def convertSubjectpage(subjectPageData: V10__DBSubjectPage): V10__DBSubjectPage = {
    parse(subjectPageData.document).toTry match {
      case Success(value) =>
        val newSubjectPage = value.hcursor
          .downField("about")
          .withFocus(
            _.mapArray(arr =>
              arr.map(
                _.hcursor
                  .downField("visualElement")
                  .downField("id")
                  .withFocus(_.mapString(replaceUrlWithId))
                  .top
                  .getOrElse(
                    throw new RuntimeException(
                      s"Something went wrong then updating about for subject page: '$subjectPageData"
                    )
                  )
              )
            )
          )
          .top
          .getOrElse {
            throw new RuntimeException(s"Something went wrong when updating about for subject page: '$subjectPageData'")
          }

        V10__DBSubjectPage(subjectPageData.id, newSubjectPage.noSpacesDropNull)
      case Failure(ex) =>
        println(s"Failed to parse subject page data for id '${subjectPageData.id}': ${ex.getMessage}")
        throw ex
    }
  }

  private def update(subjectPageData: V10__DBSubjectPage)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(subjectPageData.document)

    sql"update subjectpage set document = $dataObject where id = ${subjectPageData.id}"
      .update()
  }
}

case class V10__DBSubjectPage(id: Long, document: String)
