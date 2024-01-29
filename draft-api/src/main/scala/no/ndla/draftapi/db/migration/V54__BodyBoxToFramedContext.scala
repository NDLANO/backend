/*
 * Part of NDLA draft-api.
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migration

import io.circe.parser
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.jsoup.Jsoup
import org.jsoup.nodes.Entities.EscapeMode
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V54__BodyBoxToFramedContext extends BaseJavaMigration {

  def countAllRows(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from articledata where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
  }

  def allRows(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document, article_id from articledata where document is not null order by id limit 1000 offset $offset"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  def updateRow(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update articledata set document = $dataObject where id = $id"
      .update()
  }

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { session => migrateRows(session) }

  def migrateRows(implicit session: DBSession): Unit = {
    val count        = countAllRows.get
    var numPagesLeft = (count / 1000) + 1
    var offset       = 0L

    while (numPagesLeft > 0) {
      allRows(offset * 1000).map { case (id, document) =>
        updateRow(convertDocument(document), id)
      }: Unit
      numPagesLeft -= 1
      offset += 1
    }
  }

  private def convertHtml(str: String): String = {
    val document = Jsoup.parseBodyFragment(str)
    document
      .outputSettings()
      .escapeMode(EscapeMode.xhtml)
      .prettyPrint(false)
      .indentAmount(0)

    document
      .select("div.c-bodybox")
      .forEach(div => {
        div.removeClass("c-bodybox")
        div.attr("data-type", "framed-content"): Unit
      })

    val result = document.select("body").first().html()
    result
  }

  private[migration] def convertDocument(document: String): String = {
    val oldArticle = parser.parse(document).toTry.get
    val cursor     = oldArticle.hcursor

    if (cursor.downField("content").focus.isEmpty) {
      document
    } else {
      val newJson =
        oldArticle.hcursor
          .downField("content")
          .withFocus(f => {
            f.mapArray(contents => {
              contents.map { content =>
                content.hcursor
                  .downField("content")
                  .withFocus(f => f.mapString(str => convertHtml(str)))
                  .top
                  .get
              }
            })

          })
      newJson.top.get.noSpaces
    }
  }
}