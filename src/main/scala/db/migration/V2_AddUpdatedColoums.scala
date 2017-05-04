/*
 * Part of NDLA audio_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package db.migration

import java.sql.Connection

import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.json4s._
import org.json4s.native.JsonMethods._
import org.postgresql.util.PGobject
import scalikejdbc._

class V2_AddUpdatedColoums extends JdbcMigration {

  implicit val formats = org.json4s.DefaultFormats
  val timeService = new TimeService()

  override def migrate(connection: Connection) = {
    val db = DB(connection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allAudios.map(convertAudioUpdate).foreach(update)
    }
  }


  def allAudios(implicit session: DBSession): List[V2_DBAudioMetaInformation] = {
    sql"select id, document from audiodata".map(rs => V2_DBAudioMetaInformation(rs.long("id"), rs.string("document"))).list().apply()
  }

  def convertAudioUpdate(audioMeta: V2_DBAudioMetaInformation) = {
    val oldDocument = parse(audioMeta.document)
    val updatedJson = parse(s"""{"updatedBy": "content-import-client", "updated": "${timeService.nowAsString()}"}""")

    val mergedDoc = oldDocument merge updatedJson

    audioMeta.copy(document = compact(render(mergedDoc)))
  }


  def update(imageMeta: V2_DBAudioMetaInformation)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(imageMeta.document)

    sql"update imagemetadata set metadata = $dataObject where id = ${imageMeta.id}".update().apply
  }


}

case class V2_DBAudioMetaInformation(id: Long, document: String)


class TimeService() {
  def nowAsString(): String = {
    val formatter: DateTimeFormatter = DateTimeFormat.forPattern("YYYY-MM-DD'T'HH:mm:ssZ")
    (new DateTime).toString(formatter)
  }
}
