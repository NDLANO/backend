/*
 * Part of NDLA draft-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migration

import com.vladsch.flexmark.ext.gfm.strikethrough.SubscriptExtension
import com.vladsch.flexmark.ext.superscript.SuperscriptExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import com.vladsch.flexmark.util.misc.Extension
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

import scala.jdk.CollectionConverters._

class V55__MigrateMarkdown extends BaseJavaMigration {
  implicit val formats: DefaultFormats.type = DefaultFormats

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      migrateArticles()
    }

  private def migrateArticles()(implicit session: DBSession): Unit = {
    val count        = countAll.get
    var numPagesLeft = (count / 1000) + 1
    var offset       = 0L

    while (numPagesLeft > 0) {
      all(offset * 1000).foreach { case (id, document) =>
        updateArticle(convertDocument(document), id)
      }
      numPagesLeft -= 1
      offset += 1
    }
  }

  private def countAll(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from articledata where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
  }

  private def all(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document from articledata where document is not null order by id limit 1000 offset $offset"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  private def updateArticle(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update articledata set document = $dataObject where id = $id".update()
  }

  def updateContent(contents: JArray, contentType: String, function: String => String): json4s.JValue = {
    contents.map(content =>
      content.mapField {
        case (`contentType`, JString(html)) => (`contentType`, JString(function(html)))
        case z                              => z
      }
    )
  }

  private def stringToJsoupDocument(htmlString: String): Element = {
    val document = Jsoup.parseBodyFragment(htmlString)
    document.outputSettings().escapeMode(EscapeMode.xhtml).prettyPrint(false)
    document.select("body").first()
  }

  private def jsoupDocumentToString(element: Element): String = {
    element.select("body").html()
  }

  def fixCaption(html: String): String = {
    val doc = stringToJsoupDocument(html)
    doc
      .select("ndlaembed")
      .forEach(embed => {
        val hasCaption = embed.hasAttr("data-caption")

        if (hasCaption) {
          val oldCaption = embed.attr("data-caption")
          val newCaption = convertMarkdown(oldCaption)
          embed.attr("data-caption", newCaption): Unit
        }

      })
    jsoupDocumentToString(doc)
  }

  private def stripOuterParagraph(html: String): String = {
    val doc = stringToJsoupDocument(html).selectFirst("body")
    if (doc.childrenSize() == 1 && doc.child(0).tagName() == "p") {
      doc.child(0).unwrap()
    }
    jsoupDocumentToString(doc)
  }

  private[migration] def convertDocument(document: String): String = {
    val oldArticle = parse(document)

    val newArticle = oldArticle.mapField {
      case ("content", contents: JArray) =>
        val updated = updateContent(contents, "content", fixCaption)
        ("content", updated)
      case ("introduction", introductions: JArray) =>
        val updated = updateContent(introductions, "introduction", convertMarkdown)
        ("introduction", updated)
      case ("visualElement", contents: JArray) =>
        val updated = updateContent(contents, "visualElement", fixCaption)
        ("visualElement", updated)
      case x => x
    }

    compact(render(newArticle))
  }

  def convertMarkdown(content: String): String = {
    val options = new MutableDataSet
    options.set(HtmlRenderer.HARD_BREAK, "<br>")
    // Cast extensions to Extension before adding them to the list
    val extensions = List[Extension](SubscriptExtension.create(), SuperscriptExtension.create()).asJava
    options.set(Parser.EXTENSIONS, extensions)
    // Disable all parsers except inline
    options.set(Parser.BLOCK_QUOTE_PARSER, Boolean.box(false))
    options.set(Parser.HEADING_PARSER, Boolean.box(false))
    options.set(Parser.HTML_BLOCK_PARSER, Boolean.box(false))
    options.set(Parser.LIST_BLOCK_PARSER, Boolean.box(false))
    options.set(Parser.REFERENCE_PARAGRAPH_PRE_PROCESSOR, Boolean.box(false))
    options.set(Parser.THEMATIC_BREAK_PARSER, Boolean.box(false))
    val parser    = Parser.builder(options).build
    val converted = parser.parse(content)
    val renderer  = HtmlRenderer.builder(options).build
    stripOuterParagraph(renderer.render(converted).trim)
  }
}
