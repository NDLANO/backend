/*
 * Part of NDLA article-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s._
import org.json4s.JsonAST.{JArray, JString, JValue}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V42__MigrateOldToNewGrepCodes extends BaseJavaMigration {
  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      migrateArticles
    }

  def migrateArticles(implicit session: DBSession): Unit = {
    val count        = countAllArticles.get
    var numPagesLeft = (count / 1000) + 1
    var offset       = 0L

    while (numPagesLeft > 0) {
      allArticles(offset * 1000).map { case (id, document) =>
        updateArticle(convertArticleUpdate(document), id)
      }: Unit
      numPagesLeft -= 1
      offset += 1
    }
  }

  def countAllArticles(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from contentdata where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
  }

  def allArticles(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document, article_id from contentdata where document is not null order by id limit 1000 offset $offset"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  private val mapping = Map(
    "FSP01-02" -> "FSP01-03",
    "KV160"    -> "KV776",
    "KV161"    -> "KV777",
    "KE152"    -> "KE1427",
    "KE153"    -> "KE1428",
    "KE154"    -> "KE1429",
    "KE155"    -> "KE1430",
    "KM1829"   -> "KM10382",
    "KM1830"   -> "KM10397",
    "KM1831"   -> "KM10396",
    "KM1832"   -> "KM10395",
    "KM1833"   -> "KM10394",
    "KM1834"   -> "KM10393",
    "KM1835"   -> "KM10392",
    "KM1836"   -> "KM10391",
    "KM1837"   -> "KM10390",
    "KM1838"   -> "KM10389",
    "KM1839"   -> "KM10388",
    "KM1840"   -> "KM10387",
    "KM1841"   -> "KM10386",
    "KM1842"   -> "KM10385",
    "KM1843"   -> "KM10384",
    "KM1844"   -> "KM10383",
    "KM1845"   -> "KM10398",
    "KM1846"   -> "KM10399"
  )

  def updateArticle(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update contentdata set document = $dataObject where id = $id"
      .update()
  }
  def convertGrepCode(oldCode: String): String = mapping.getOrElse(oldCode, oldCode)

  def convertGrepCodes(codes: JArray): JValue =
    codes.map {
      case JString(code) => JString(convertGrepCode(code))
      case x             => x
    }

  private[migration] def convertArticleUpdate(document: String): String = {
    val oldArticle = parse(document)
    val newArticle = oldArticle.mapField {
      case ("grepCodes", oldGrepCodes: JArray) =>
        val newGrepCodes = convertGrepCodes(oldGrepCodes)
        ("grepCodes", newGrepCodes)
      case x => x
    }

    compact(render(newArticle))
  }
}
