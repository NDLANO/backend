/*
 * Part of NDLA draft-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s
import org.json4s.DefaultFormats
import org.json4s.JsonAST.{JArray, JString}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities.EscapeMode
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V32__FullbreddeImagesToFull extends BaseJavaMigration {
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

  private def stringToJsoupDocument(htmlString: String): Element = {
    val document = Jsoup.parseBodyFragment(htmlString)
    document.outputSettings().escapeMode(EscapeMode.xhtml).prettyPrint(false)
    document.select("body").first()
  }

  private def jsoupDocumentToString(element: Element): String = {
    element.select("body").html()
  }

  def fixImageSizesInHtml(html: String): String = {
    val doc = stringToJsoupDocument(html)
    doc
      .select("embed")
      .forEach(embed => {
        val dataResource = embed.attr("data-resource")
        val isImage = dataResource == "image"
        val hasSize = embed.hasAttr("data-size")

        if (isImage && hasSize) {
          val oldSize = embed.attr("data-size")
          val newSize = oldSize.replace("fullbredde", "full")
          embed.attr("data-size", newSize)
        }

      })
    jsoupDocumentToString(doc)
  }

  def updateContent(contents: JArray, contentType: String): json4s.JValue = {
    contents.map(content =>
      content.mapField {
        case (`contentType`, JString(html)) => (`contentType`, JString(fixImageSizesInHtml(html)))
        case z                              => z
    })
  }

  private[migration] def convertArticleUpdate(document: String): String = {
    val oldArticle = parse(document)

    val newArticle = oldArticle.mapField {
      case ("content", contents: JArray) =>
        val updatedContent = updateContent(contents, "content")
        ("content", updatedContent)
      case ("visualElement", visualElements: JArray) =>
        val updatedVisualElement = updateContent(visualElements, "resource")
        ("visualElement", updatedVisualElement)
      case x => x
    }

    compact(render(newArticle))
  }
}
