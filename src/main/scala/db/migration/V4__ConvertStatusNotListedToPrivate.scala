/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package db.migration

import java.sql.Connection

import no.ndla.learningpathapi.model.domain.LearningPathStatus
import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.json4s._
import org.json4s.native.JsonMethods._
import org.postgresql.util.PGobject
import scalikejdbc._

class V4__ConvertStatusNotListedToPrivate extends JdbcMigration {

  implicit val formats = org.json4s.DefaultFormats

  override def migrate(connection: Connection) = {
    val db = DB(connection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allLearningPaths.map(convertLearningPathStatus).foreach(update)
    }
  }

  def allLearningPaths(implicit session: DBSession): List[V4_DBLearningPath] = {
    sql"select id, document from learningpaths".map(rs => V4_DBLearningPath(rs.long("id"), rs.string("document"))).list().apply()
  }

  def convertLearningPathStatus(learningPath: V4_DBLearningPath): V4_DBLearningPath = {
    val oldDocument = parse(learningPath.document)
    val updatedDocument = oldDocument mapField {
      case ("status", JString(oldStatus)) => {
        if (oldStatus == "NOT_LISTED") {
          ("status", JString("PRIVATE"))
        } else {
          ("status", JString(oldStatus))
        }
      }
      case x => x
    }
    learningPath.copy(document = compact(render(updatedDocument)))
  }

  def update(learningPath: V4_DBLearningPath)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(learningPath.document)

    sql"update learningpaths set document = $dataObject where id = ${learningPath.id}".update().apply
  }
}

case class V4_DBLearningPath(id: Long, document: String)
