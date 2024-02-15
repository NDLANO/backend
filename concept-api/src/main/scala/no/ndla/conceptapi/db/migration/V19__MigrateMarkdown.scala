/*
 * Part of NDLA concept-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.{DefaultFormats, Extraction}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

import scala.collection.immutable.ListMap
import scala.util.matching.Regex

class V19__MigrateMarkdown extends BaseJavaMigration {
  implicit val formats: DefaultFormats.type = DefaultFormats

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      migrateConcepts()
      migratePublishedConcepts()
    }

  private def migratePublishedConcepts()(implicit session: DBSession): Unit = {
    val count        = countAllPublishedConcepts.get
    var numPagesLeft = (count / 1000) + 1
    var offset       = 0L

    while (numPagesLeft > 0) {
      allPublishedConcepts(offset * 1000).foreach { case (id, document) =>
        updatePublishedConcept(convertToNewConcept(document), id)
      }
      numPagesLeft -= 1
      offset += 1
    }
  }

  private def migrateConcepts()(implicit session: DBSession): Unit = {
    val count        = countAllConcepts.get
    var numPagesLeft = (count / 1000) + 1
    var offset       = 0L

    while (numPagesLeft > 0) {
      allConcepts(offset * 1000).foreach { case (id, document) =>
        updateConcept(convertToNewConcept(document), id)
      }
      numPagesLeft -= 1
      offset += 1
    }
  }

  private def countAllPublishedConcepts(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from publishedconceptdata where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
  }

  private def countAllConcepts(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from conceptdata where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
  }

  private def allPublishedConcepts(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document from publishedconceptdata where document is not null order by id limit 1000 offset $offset"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  private def allConcepts(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document from conceptdata where document is not null order by id limit 1000 offset $offset"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  private def updatePublishedConcept(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update publishedconceptdata set document = $dataObject where id = $id"
      .update()
  }

  private def updateConcept(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update conceptdata set document = $dataObject where id = $id".update()
  }

  private def convertMarkdown(content: V19_ConceptContent): V19_ConceptContent = {
    val patterns = ListMap(
      "~"      -> "sub",
      "\\^"    -> "sup",
      "\\*\\*" -> "strong",
      "__"     -> "strong",
      "\\*"    -> "em",
      "_"      -> "em"
    )
    var new_text = content.content
    for ((pattern, tag) <- patterns) {
      val regex = new Regex(s"$pattern(.*?)$pattern")
      new_text = regex.replaceAllIn(new_text, m => s"<$tag>${m.group(1)}</$tag>")
    }
    content.copy(content = new_text)
  }

  def convertToNewConcept(document: String): String = {
    val concept = parse(document)
    val content = (concept \ "content").extract[Seq[V19_ConceptContent]]

    val convertedContent = content.map(convertMarkdown)
    val newConcept       = concept.replace(List("content"), Extraction.decompose(convertedContent))

    compact(render(newConcept))
  }

  private case class V19_ConceptContent(content: String, language: String)
}
