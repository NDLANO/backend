/*
 * Part of NDLA concept-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.JsonAST.JObject
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.{DefaultFormats, Extraction}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V13__RenameStatuses extends BaseJavaMigration {
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
        updatePublishedConcept(convertToNewConcept(document), id)
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
        updateConcept(convertToNewConcept(document), id)
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

  def convertStatus(oldStatus: String): String = {
    oldStatus match {
      case "DRAFT"               => "IN_PROGRESS"
      case "QUALITY_ASSURED"     => "END_CONTROL"
      case "QUEUED_FOR_LANGUAGE" => "LANGUAGE"
      case "TRANSLATED"          => "FOR_APPROVAL"
      case x                     => x
    }
  }

  def convertToNewConcept(document: String): String = {
    val concept = parse(document)
    val newConcept = concept
      .mapField {
        case ("status", status: JObject) =>
          val oldStatus = status.extract[V13__Status]
          val newStatus =
            oldStatus.copy(current = convertStatus(oldStatus.current), other = oldStatus.other.map(convertStatus))
          ("status", Extraction.decompose(newStatus))
        case x => x
      }
    compact(render(newConcept))
  }

}
case class V13__Status(current: String, other: Seq[String])
