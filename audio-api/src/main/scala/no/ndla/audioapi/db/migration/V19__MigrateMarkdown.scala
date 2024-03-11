/*
 * Part of NDLA audio-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.db.migration

import com.vladsch.flexmark.ext.gfm.strikethrough.SubscriptExtension
import com.vladsch.flexmark.ext.superscript.SuperscriptExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import com.vladsch.flexmark.util.misc.Extension
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s
import org.json4s.*
import org.json4s.JsonAST.{JArray, JString}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities.EscapeMode
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

import scala.jdk.CollectionConverters._

class V19__MigrateMarkdown extends BaseJavaMigration {
  implicit val formats: DefaultFormats.type = DefaultFormats

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      migrateAll()
    }

  private def migrateAll()(implicit session: DBSession): Unit = {
    val count        = countAll.get
    var numPagesLeft = (count / 1000) + 1
    var offset       = 0L

    while (numPagesLeft > 0) {
      all(offset * 1000).foreach { case (id, document) =>
        updateAudio(convertDocument(document), id)
      }
      numPagesLeft -= 1
      offset += 1
    }
  }

  private def countAll(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from audiodata where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
  }

  private def all(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document from audiodata where document is not null order by id limit 1000 offset $offset"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  private def updateAudio(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update audiodata set document = $dataObject where id = $id".update()
  }

  def updateContent(contents: JArray, contentType: String): json4s.JValue = {
    contents.map(content =>
      content.mapField {
        case (`contentType`, JString(html)) => (`contentType`, JString(convertMarkdown(html)))
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

  private def stripOuterParagraph(html: String): String = {
    val doc = stringToJsoupDocument(html).selectFirst("body")
    // Remove outer paragraph if it's the only child
    if (doc.childrenSize() == 1 && doc.child(0).tagName() == "p") {
      doc.child(0).unwrap()
    }
    // Remove empty paragraphs
    for (child <- doc.children().asScala) {
      if (child.tagName() == "p" && child.text().isEmpty) {
        child.remove()
      }
    }
    jsoupDocumentToString(doc)
  }

  private[migration] def convertDocument(document: String): String = {
    val old = parse(document)

    val newArticle = old.mapField {
      case ("manuscript", introductions: JArray) =>
        val updated = updateContent(introductions, "manuscript")
        ("manuscript", updated)
      case x => x
    }

    compact(render(newArticle))
  }

  private def convertMarkdown(content: String): String = {
    val options = new MutableDataSet()
    options.set(HtmlRenderer.HARD_BREAK, "<br>")
    options.set(HtmlRenderer.SOFT_BREAK, "")
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
    val parser   = Parser.builder(options).build
    val renderer = HtmlRenderer.builder(options).build

    val converted = parser.parse(content)
    stripOuterParagraph(renderer.render(converted).trim)
  }
}
