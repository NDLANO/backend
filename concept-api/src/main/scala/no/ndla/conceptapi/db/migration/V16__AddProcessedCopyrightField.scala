/*
 * Part of NDLA concept-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.db.migration

import io.circe.Json
import io.circe.parser.parse
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V16__AddProcessedCopyrightField extends BaseJavaMigration {
  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      migrateConcepts()
      migratePublishedConcepts()
    }

  def migratePublishedConcepts()(implicit session: DBSession): Unit = {
    val count        = countAllPublishedConcepts.get
    var numPagesLeft = (count / 1000) + 1
    var offset       = 0L

    while (numPagesLeft > 0) {
      allPublishedConcepts(offset * 1000).foreach { case (id, document) =>
        updatePublishedConcept(convertDocument(document), id)
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

  def migrateConcepts()(implicit session: DBSession): Unit = {
    val count        = countAllConcepts.get
    var numPagesLeft = (count / 1000) + 1
    var offset       = 0L

    while (numPagesLeft > 0) {
      allConcepts(offset * 1000).foreach { case (id, document) =>
        updateConcept(convertDocument(document), id)
      }
      numPagesLeft -= 1
      offset += 1
    }
  }

  def updateConcept(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update conceptdata set document = $dataObject where id = $id"
      .update()
  }

  def updatePublishedConcept(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update publishedconceptdata set document = $dataObject where id = $id"
      .update()
  }

  private[migration] def convertDocument(document: String): String = {
    val oldArticle = parse(document).toTry.get
    val cursor     = oldArticle.hcursor
    val copyrightCursor = cursor
      .downField("copyright")
      .withFocus(_.mapObject(obj => {
        obj.add("processed", Json.False)
      }))

    copyrightCursor.top.map(_.noSpaces).getOrElse(document)
  }
}
