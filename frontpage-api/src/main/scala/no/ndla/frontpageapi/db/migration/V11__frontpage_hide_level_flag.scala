/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.db.migration

import io.circe.parser.parse
import io.circe.{Json, JsonObject}
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.postgresql.util.PGobject
import scalikejdbc._

import scala.util.{Failure, Success}

class V11__frontpage_hide_level_flag extends BaseJavaMigration {

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      frontPageData
        .map(convertFrontpage)
        .foreach(update)
    }

  private def frontPageData(implicit session: DBSession): List[V11__DBFrontPage] = {
    sql"select id, document from mainfrontpage"
      .map(rs => V11__DBFrontPage(rs.long("id"), rs.string("document")))
      .list()
  }

  implicit class JsonObjectOps(obj: JsonObject) {
    def addIfNotExists(key: String, value: Json): JsonObject = {
      if (obj.contains(key)) obj
      else obj.add(key, value)
    }
  }

  private[migration] def convertFrontpage(frontPageData: V11__DBFrontPage): V11__DBFrontPage = {
    parse(frontPageData.document).toTry match {
      case Success(value) =>
        val updatedJson = updateMenu(value)
        V11__DBFrontPage(frontPageData.id, updatedJson.noSpaces)
      case Failure(error) =>
        println(s"Something went wrong then updating frontpage: '$frontPageData.id")
        throw error
    }
  }

  private def updateMenu(json: Json): Json = {
    def addHideLevelFlag(obj: Json): Json = {
      val withFlag  = obj.mapObject(_.add("hideLevel", Json.False))
      val maybeMenu = obj.hcursor.downField("menu").focus

      maybeMenu match {
        case Some(menuJson) if menuJson.isArray =>
          val updatedMenu = menuJson.asArray.get.map(addHideLevelFlag)
          withFlag.mapObject(_.add("menu", Json.fromValues(updatedMenu)))
        case _ => withFlag
      }
    }

    addHideLevelFlag(json).mapObject(_.remove("hideLevel"))
  }
  private def update(frontPageData: V11__DBFrontPage)(implicit session: DBSession): Unit = {
    val pgObject = new PGobject()
    pgObject.setType("json")
    pgObject.setValue(frontPageData.document)
    sql"update mainfrontpage set document = $pgObject where id = ${frontPageData.id}"
      .update()
  }
}

case class V11__DBFrontPage(id: Long, document: String)
