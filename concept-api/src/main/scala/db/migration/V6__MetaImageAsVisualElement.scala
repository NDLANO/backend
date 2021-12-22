/*
 * Part of NDLA concept-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.JsonAST.{JArray, JObject}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.{DefaultFormats, Extraction}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V6__MetaImageAsVisualElement extends BaseJavaMigration {
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
    val count = countAllPublishedConcepts.get
    var numPagesLeft = (count / 1000) + 1
    var offset = 0L

    while (numPagesLeft > 0) {
      allPublishedConcepts(offset * 1000).map {
        case (id, document) => updatePublishedConcept(convertToNewConcept(document), id)
      }
      numPagesLeft -= 1
      offset += 1
    }
  }

  def migrateConcepts()(implicit session: DBSession): Unit = {
    val count = countAllConcepts.get
    var numPagesLeft = (count / 1000) + 1
    var offset = 0L

    while (numPagesLeft > 0) {
      allConcepts(offset * 1000).map {
        case (id, document) => updateConcept(convertToNewConcept(document), id)
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

  private def mergeFields(
      existing: Seq[NewVisualElement],
      updated: Seq[NewVisualElement]
  ): Seq[NewVisualElement] = {
    val toKeep = existing.filterNot(item => updated.map(_.language).contains(item.language))
    (toKeep ++ updated).filterNot(_.visualElement.isEmpty)
  }

  def convertMetaImageToVisualElement(image: OldMetaImage) = {
    val embedString =
      s"""<embed data-resource="image" data-resource_id="${image.imageId}" data-alt="${image.altText}" data-size="full" data-align="" />"""
    NewVisualElement(embedString, image.language)
  }

  def convertToNewConcept(document: String): String = {
    val concept = parse(document)
    val metaImages = (concept \ "metaImage").extract[Seq[OldMetaImage]]
    val visualElements = (concept \ "visualElement").extract[Seq[NewVisualElement]]

    val convertedVisualElements = metaImages.map(convertMetaImageToVisualElement)

    val newVisualElements = mergeFields(convertedVisualElements, visualElements)
    val newConcept = concept.merge(JObject("visualElement" -> Extraction.decompose(newVisualElements)))

    compact(render(newConcept))
  }

  case class OldMetaImage(imageId: String, altText: String, language: String)
  case class NewVisualElement(visualElement: String, language: String)
}
