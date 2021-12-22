/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.JsonAST.JString
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.{DefaultFormats, JObject}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V10__UpdateLicenses extends BaseJavaMigration {
  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats

  override def migrate(context: Context): Unit = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      migrateArticles
    }
  }

  def migrateArticles(implicit session: DBSession): Unit = {
    val count = countAllArticles.get
    var numPagesLeft = (count / 1000) + 1
    var offset = 0L

    while (numPagesLeft > 0) {
      allArticles(offset * 1000).map {
        case (id, document) => updateArticle(convertArticleUpdate(document), id)
      }
      numPagesLeft -= 1
      offset += 1
    }
  }

  def countAllArticles(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from articledata where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
  }

  def allArticles(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document from articledata where document is not null order by id limit 1000 offset ${offset}"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  def updateLicense(license: String): String = {
    val mapping = Map(
      "by" -> "CC-BY-4.0",
      "by-sa" -> "CC-BY-SA-4.0",
      "by-nc" -> "CC-BY-NC-4.0",
      "by-nd" -> "CC-BY-ND-4.0",
      "by-nc-sa" -> "CC-BY-NC-SA-4.0",
      "by-nc-nd" -> "CC-BY-NC-ND-4.0",
      "by-3.0" -> "CC-BY-4.0",
      "by-sa-3.0" -> "CC-BY-SA-4.0",
      "by-nc-3.0" -> "CC-BY-NC-4.0",
      "by-nd-3.0" -> "CC-BY-ND-4.0",
      "by-nc-sa-3.0" -> "CC-BY-NC-SA-4.0",
      "by-nc-nd-3.0" -> "CC-BY-NC-ND-4.0",
      "cc0" -> "CC0-1.0",
      "pd" -> "PD",
      "copyrighted" -> "COPYRIGHTED"
    )

    mapping.getOrElse(license, license)
  }

  def convertArticleUpdate(document: String): String = {
    val oldArticle = parse(document)

    val newArticle = oldArticle.mapField {
      case ("copyright", copyright: JObject) =>
        "copyright" -> copyright.mapField {
          case ("license", license: JString) =>
            "license" -> JString(updateLicense(license.values))
          case x => x
        }
      case x => x
    }
    compact(render(newArticle))
  }

  def updateArticle(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update articledata set document = ${dataObject} where id = ${id}"
      .update()
  }

  case class V8_Status(current: String, other: Set[String])
}
