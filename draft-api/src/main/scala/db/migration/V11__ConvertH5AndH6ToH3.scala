/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.JsonAST.{JInt, JString}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.{DefaultFormats, JArray, JObject}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities.EscapeMode
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}
import scala.jdk.CollectionConverters._

class V11__ConvertH5AndH6ToH3 extends BaseJavaMigration {
  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats

  override def migrate(context: Context): Unit = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      migrateArticles
    }
  }

  private def migrateArticles(implicit session: DBSession): Unit = {
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

  private def countAllArticles(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from articledata where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
  }

  private def allArticles(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document from articledata where document is not null order by id limit 1000 offset ${offset}"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  private def stringToJsoupDocument(htmlString: String): Element = {
    val document = Jsoup.parseBodyFragment(htmlString)
    document.outputSettings().escapeMode(EscapeMode.xhtml).prettyPrint(false)
    document.select("body").first()
  }

  private def jsoupDocumentToString(element: Element): String = {
    element.select("body").html()
  }

  private def removeH5AndH6(html: String): String = {
    val doc = stringToJsoupDocument(html)
    doc
      .select("h5,h6")
      .asScala
      .foreach(tag => {
        tag.tagName match {
          case "h5" => tag.tagName("strong").wrap("<p></p>")
          case "h6" => tag.unwrap()
        }
      })
    jsoupDocumentToString(doc)
  }

  private[migration] def convertArticleUpdate(document: String): String = {
    val oldArticle = parse(document)

    val newArticle = oldArticle.mapField {
      case ("content", contents: JArray) =>
        val updatedContent = contents.map {
          case content: JObject =>
            content.mapField {
              case ("content", JString(html)) => ("content", JString(removeH5AndH6(html)))
              case z                          => z
            }
          case y => y
        }
        ("content", updatedContent)
      case x => x
    }

    compact(render(newArticle))
  }

  private def updateArticle(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update articledata set document = ${dataObject} where id = ${id}"
      .update()
  }

}
