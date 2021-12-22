/*
 * Part of NDLA concept-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.conceptapi.model.api.{ConceptContent, ConceptTags}
import no.ndla.conceptapi.model.domain.{ConceptMetaImage, ConceptTitle, VisualElement}
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.{DefaultFormats, Extraction}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, scalikejdbcSQLInterpolationImplicitDef}

class V9__LanguageUnknownToUnd extends BaseJavaMigration {
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

  def convertToNewConcept(document: String): String = {
    val concept = parse(document)
    val titles = (concept \ "title")
      .extract[Seq[ConceptTitle]]
      .map(t => {
        if (t.language == "unknown")
          t.copy(language = "und")
        else
          t
      })
    val content = (concept \ "content")
      .extract[Seq[ConceptContent]]
      .map(c => {
        if (c.language == "unknown")
          c.copy(language = "und")
        else
          c
      })
    val tags = (concept \ "tags")
      .extract[Seq[ConceptTags]]
      .map(t => {
        if (t.language == "unknown")
          t.copy(language = "und")
        else
          t
      })
    val metaImage = (concept \ "metaImage")
      .extract[Seq[ConceptMetaImage]]
      .map(m => {
        if (m.language == "unknown")
          m.copy(language = "und")
        else
          m
      })
    val visualElement = (concept \ "visualElement")
      .extract[Seq[VisualElement]]
      .map(t => {
        if (t.language == "unknown")
          t.copy(language = "und")
        else
          t
      })

    val newConcept = concept
      .replace(List("title"), Extraction.decompose(titles))
      .replace(List("content"), Extraction.decompose(content))
      .replace(List("tags"), Extraction.decompose(tags))
      .replace(List("metaImage"), Extraction.decompose(metaImage))
      .replace(List("visualElement"), Extraction.decompose(visualElement))

    compact(render(newConcept))
  }
}
