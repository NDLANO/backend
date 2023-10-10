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

import scala.util.Success

class V17__MoveSourceToOrigin extends BaseJavaMigration {
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

  private def getNewOriginFromSource(source: String): String = source.trim match {
    case s"[$_]($url)" => url
    case unmatched     => unmatched
  }

  private[migration] def convertDocument(document: String): String = {
    val oldArticle = parse(document).toTry.get
    val cursor     = oldArticle.hcursor
    cursor.downField("source").as[Option[String]] match {
      case Right(Some(sourceField)) if sourceField.isEmpty => cursor.downField("source").delete.top.get.noSpaces
      case Right(Some(markdownOrStringSource)) =>
        cursor.up.downField("copyright").downField("origin").as[Option[String]].toTry match {
          case Success(Some(existingOrigin)) if existingOrigin.nonEmpty =>
            // If origin already exists we don't mess with the origin and just delete the old source
            oldArticle.hcursor.downField("source").delete.top.get.noSpaces
          case _ =>
            // If origin isn't set, or is set to some empty string, we can override it with data from source
            val newOrigin = getNewOriginFromSource(markdownOrStringSource)

            oldArticle.hcursor
              .downField("source")
              .delete
              .downField("copyright")
              .withFocus(_.mapObject(_.add("origin", Json.fromString(newOrigin)))) // Add origin field in copyright
              .top
              .get
              .noSpaces
        }

      case _ => document // Dont make any modifications if source isn't extracted
    }
  }
}
