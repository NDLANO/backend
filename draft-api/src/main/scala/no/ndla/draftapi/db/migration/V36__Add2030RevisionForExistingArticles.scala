/*
 * Part of NDLA draft-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.ext.JavaTimeSerializers
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.{DefaultFormats, Extraction, Formats, JArray, JField, JObject, JString}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

import java.time.{LocalDateTime, Month}

class V36__Add2030RevisionForExistingArticles extends BaseJavaMigration {
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
      allArticles(offset * 1000).foreach { case (id, document) =>
        updateArticle(convertArticleUpdate(document), id)
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
    sql"select id, document, article_id from articledata where document is not null order by id limit 1000 offset $offset"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  def updateArticle(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update articledata set document = $dataObject where id = $id"
      .update()
  }

  private val revisionDate              = LocalDateTime.of(2030, Month.JANUARY, 1, 0, 0)
  private implicit val formats: Formats = DefaultFormats.withLong ++ JavaTimeSerializers.all
  private val revDate                   = Extraction.decompose(revisionDate)

  private[migration] def convertArticleUpdate(document: String): String = {
    val oldArticle = parse(document)

    val note = JString("Automatisk revisjonsdato satt av systemet.")
    val mergeObject = JObject(
      "revisionMeta" -> JArray(
        List(
          JObject(
            JField("revisionDate", revDate),
            JField("note", note),
            JField("status", JString("needs-revision"))
          )
        )
      )
    )
    val updated = oldArticle.merge(mergeObject)
    compact(render(updated))
  }
}
