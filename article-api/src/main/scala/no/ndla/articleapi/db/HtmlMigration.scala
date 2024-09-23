/*
 * Part of NDLA article-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.db

import io.circe.parser
import io.circe.syntax.EncoderOps
import no.ndla.common.model.domain.article.Article
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities.EscapeMode
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, *}

abstract class HtmlMigration extends BaseJavaMigration {
  private def countAllRows(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from contentdata where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
  }

  private def allRows(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document, article_id from contentdata where document is not null order by id limit 1000 offset $offset"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  private def updateRow(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update contentdata set document = $dataObject where id = $id"
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

  def convertContent(htmlString: String, language: String): String = {
    val doc       = stringToJsoupDocument(htmlString)
    val converted = convertHtml(doc, language)
    jsoupDocumentToString(converted)
  }

  protected def convertArticleUpdate(document: String): String = {
    val oldArticle = parser.parse(document).flatMap(_.as[Article]).toTry.get
    val convertedContent = oldArticle.content.map(c => {
      val converted = convertContent(c.content, c.language)
      c.copy(content = converted)
    })

    val convertedVisualElement = if (convertVisualElement) {
      oldArticle.visualElement.map(ve => {
        val doc       = stringToJsoupDocument(ve.resource)
        val converted = convertHtml(doc, ve.language)
        ve.copy(resource = jsoupDocumentToString(converted))
      })
    } else oldArticle.visualElement

    val newArticle = oldArticle.copy(content = convertedContent, visualElement = convertedVisualElement)
    newArticle.asJson.noSpaces
  }

  val convertVisualElement: Boolean

  /** Method to override that manipulates the content string */
  def convertHtml(doc: Element, language: String): Element
}
