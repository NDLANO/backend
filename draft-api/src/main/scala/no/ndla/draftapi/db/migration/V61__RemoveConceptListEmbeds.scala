/*
 * Part of NDLA draft-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.db.migration

import io.circe.parser
import io.circe.syntax.EncoderOps
import no.ndla.common.model.domain.ArticleContent
import no.ndla.common.model.domain.draft.Draft
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities.EscapeMode
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, *}

class V61__RemoveConceptListEmbeds extends BaseJavaMigration {
  private def countAllRows(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from articledata where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
  }

  private def allRows(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document, article_id from articledata where document is not null order by id limit 1000 offset $offset"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  private def updateRow(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update articledata set document = $dataObject where id = $id"
      .update()
  }

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { session => migrateRows(session) }

  private def migrateRows(implicit session: DBSession): Unit = {
    val count        = countAllRows.get
    var numPagesLeft = (count / 1000) + 1
    var offset       = 0L

    while (numPagesLeft > 0) {
      allRows(offset * 1000).map { case (id, document) =>
        updateRow(convertArticleUpdate(document), id)
      }: Unit
      numPagesLeft -= 1
      offset += 1
    }
  }

  private def stringToJsoupDocument(htmlString: String): Element = {
    val document = Jsoup.parseBodyFragment(htmlString)
    document.outputSettings().escapeMode(EscapeMode.xhtml).prettyPrint(false)
    document.select("body").first()
  }

  private def jsoupDocumentToString(element: Element): String = {
    element.select("body").html()
  }

  def convertContent(c: ArticleContent): ArticleContent = {
    val doc = stringToJsoupDocument(c.content)

    doc
      .select("ndlaembed")
      .forEach(embed => {
        val dataResource = embed.attr("data-resource")
        if (dataResource == "concept-list") {
          embed.remove()
        }
      })
    c.copy(content = jsoupDocumentToString(doc))
  }

  private[migration] def convertArticleUpdate(document: String): String = {
    val oldArticle = parser.parse(document).flatMap(_.as[Draft]).toTry.get

    val converted = oldArticle.content.map(c => convertContent(c))

    val newArticle = oldArticle.copy(content = converted)
    newArticle.asJson.noSpaces
  }
}
