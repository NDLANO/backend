/*
 * Part of NDLA concept-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package conceptapi.db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.JsonAST.JArray
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.{DefaultFormats, Extraction}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities.EscapeMode
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V10__RemoveImageVisualElementsWithoutIds extends BaseJavaMigration {
  implicit val formats: DefaultFormats.type = DefaultFormats

  override def migrate(context: Context): Unit = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      migrateConcepts()
      migratePublishedConcepts()
    }
  }

  def migratePublishedConcepts()(implicit session: DBSession): Unit = {
    val count        = countAllPublishedConcepts.get
    var numPagesLeft = (count / 1000) + 1
    var offset       = 0L

    while (numPagesLeft > 0) {
      allPublishedConcepts(offset * 1000).map { case (id, document) =>
        updatePublishedConcept(convertToNewConcept(document, id), id)
      }
      numPagesLeft -= 1
      offset += 1
    }
  }

  def migrateConcepts()(implicit session: DBSession): Unit = {
    val count        = countAllConcepts.get
    var numPagesLeft = (count / 1000) + 1
    var offset       = 0L

    while (numPagesLeft > 0) {
      allConcepts(offset * 1000).map { case (id, document) =>
        updateConcept(convertToNewConcept(document, id), id)
      }
      numPagesLeft -= 1
      offset += 1
    }
  }

  def countAllPublishedConcepts(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from publishedconceptdata where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
  }

  def countAllConcepts(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from conceptdata where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
  }

  def allPublishedConcepts(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document from publishedconceptdata where document is not null order by id limit 1000 offset $offset"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  def allConcepts(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document from conceptdata where document is not null order by id limit 1000 offset $offset"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  def updatePublishedConcept(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update publishedconceptdata set document = $dataObject where id = $id"
      .update()
  }

  def updateConcept(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update conceptdata set document = $dataObject where id = $id"
      .update()
  }

  private def stringToJsoupDocument(htmlString: String): Element = {
    val document = Jsoup.parseBodyFragment(htmlString)
    document.outputSettings().escapeMode(EscapeMode.xhtml).prettyPrint(false)
    document.select("body").first()
  }

  def convertVisualElement(oldVisualElement: NewVisualElement, id: Long): Option[NewVisualElement] = {
    if (oldVisualElement.visualElement.nonEmpty) {
      val x     = stringToJsoupDocument(oldVisualElement.visualElement)
      val embed = Option(x.select("embed").first())
      embed match {
        case Some(oldEmbed) =>
          val resourceType = oldEmbed.attr("data-resource")
          val imageId      = oldEmbed.attr("data-resource_id")
          if (resourceType == "image" && imageId.isEmpty) {
            println(
              s"Concept '$id' had empty-id visualelement image in language '${oldVisualElement.language}', removing..."
            )
            None
          } else {
            Some(oldVisualElement)
          }
        case _ => Some(oldVisualElement)

      }
    } else {
      Some(oldVisualElement)
    }
  }

  def convertToNewConcept(document: String, id: Long): String = {
    val concept = parse(document)
    val newConcept = concept
      .mapField {
        case ("visualElement", visualElement: JArray) =>
          val visualElements    = visualElement.extract[Seq[NewVisualElement]]
          val newVisualElements = visualElements.flatMap(ve => convertVisualElement(ve, id))
          "visualElement" -> Extraction.decompose(newVisualElements)
        case x => x
      }
    compact(render(newConcept))
  }

  case class NewVisualElement(visualElement: String, language: String)
}
