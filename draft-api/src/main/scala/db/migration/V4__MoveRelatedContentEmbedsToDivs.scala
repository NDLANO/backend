/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.Extraction.decompose
import org.json4s.JsonAST.JArray
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.{DefaultFormats, JValue}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities.EscapeMode
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

import scala.jdk.CollectionConverters._

class V4__MoveRelatedContentEmbedsToDivs extends BaseJavaMigration {

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
    sql"select id, document from articledata where document is not null order by id limit 1000 offset ${offset}"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  private def convertContent(content: List[V4_Content]): JValue = {
    val contents = content.map(cont => {
      val document = Jsoup.parseBodyFragment(cont.content)
      document
        .outputSettings()
        .escapeMode(EscapeMode.xhtml)
        .prettyPrint(false)
        .indentAmount(0)

      for (embed <- document.select("embed[data-resource='related-content']").asScala) {
        val ids = embed.attr("data-article-ids").split(',').filterNot(_ == "")

        // If ids are empty, we assume the embed is already converted
        if (ids.nonEmpty) {
          val newEmbedDiv =
            new Element("div").attr("data-type", "related-content")
          embed.after(newEmbedDiv)

          ids.map(id => {
            newEmbedDiv
              .appendElement("embed")
              .attr("data-article-id", id)
              .attr("data-resource", "related-content")
          })

          embed.remove()
        }
      }

      val newContentString = document.select("body").first().html()
      cont.copy(content = newContentString)
    })

    decompose(contents)
  }

  def convertArticleUpdate(document: String): String = {
    val oldArticle = parse(document)

    val newArticle = oldArticle.mapField {
      case ("content", content: JArray) =>
        "content" -> convertContent(content.extract[List[V4_Content]])
      case x => x
    }
    compact(render(newArticle))
  }

  def updateArticle(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update articledata set document = ${dataObject} where id = ${id}"
      .update()
  }

  case class V4_Content(content: String, language: String)
}
