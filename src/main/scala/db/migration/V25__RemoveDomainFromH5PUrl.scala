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

class V25__RemoveDomainFromH5PUrl extends BaseJavaMigration {
  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats

  override def migrate(context: Context) = {
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

  def containsH5PLink(url: String): Boolean = {
    val h5pUrls =
      Seq("https://h5p.ndla.no", "https://h5p-test.ndla.no", "https://h5p-ff.ndla.no", "https://h5p-staging.ndla.no")
    val delUrl = h5pUrls.find(u => url.contains(u))

    delUrl match {
      case Some(_) => true;
      case None    => false
    }
  }

  def relativeUrl(url: String): String = {
    url.replaceAll("https?://h5p.{0,8}.ndla.no", "")
  }

  def updateContent(html: String): String = {
    val doc = stringToJsoupDocument(html)
    doc
      .select("embed")
      .forEach(embed => {
        val dataResource = embed.attr("data-resource")
        if (dataResource == "external") {
          val url = embed.attr("data-url")
          if (containsH5PLink(url)) {
            embed.attr("data-path", relativeUrl(url))
            embed.attr("data-resource", "h5p")
            embed.removeAttr("data-url")
          }
        }
      })
    jsoupDocumentToString(doc)
  }

  def updateContent(contents: JArray, contentType: String): json4s.JValue = {
    contents.map(content =>
      content.mapField {
        case (`contentType`, JString(html)) => (`contentType`, JString(updateContent(html)))
        case z                              => z
    })
  }

  private[migration] def convertArticleUpdate(document: String): String = {
    val oldArticle = parse(document)

    val newArticle = oldArticle.mapField {
      case ("visualElement", visualElements: JArray) => {
        val updatedContent = updateContent(visualElements, "resource")
        ("visualElement", updatedContent)
      }
      case ("content", contents: JArray) => {
        val updatedContent = updateContent(contents, "content")
        ("content", updatedContent)
      }
      case x => x
    }

    compact(render(newArticle))
  }

}
