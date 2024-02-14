/*
 * Part of NDLA audio-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.Formats
import org.json4s.native.JsonMethods._
import org.postgresql.util.PGobject
import scalikejdbc._

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class V2__AddUpdatedColoums extends BaseJavaMigration {

  implicit val formats: Formats = org.json4s.DefaultFormats
  val timeService               = new TimeService()

  override def migrate(context: Context) = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      allAudios.map(convertAudioUpdate).foreach(update)
    }

  def allAudios(implicit session: DBSession): List[V2_DBAudioMetaInformation] = {
    sql"select id, document from audiodata"
      .map(rs => V2_DBAudioMetaInformation(rs.long("id"), rs.string("document")))
      .list()
  }

  def convertAudioUpdate(audioMeta: V2_DBAudioMetaInformation): V2_DBAudioMetaInformation = {
    val oldDocument = parse(audioMeta.document)
    val updatedJson = parse(s"""{"updatedBy": "content-import-client", "updated": "${timeService.nowAsString()}"}""")

    val mergedDoc = oldDocument merge updatedJson

    audioMeta.copy(document = compact(render(mergedDoc)))
  }

  def update(audioMeta: V2_DBAudioMetaInformation)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(audioMeta.document)

    sql"update audiodata set document = $dataObject where id = ${audioMeta.id}".update()
  }

}

case class V2_DBAudioMetaInformation(id: Long, document: String)

class TimeService() {

  def nowAsString(): String = {
    val currentTime = LocalDateTime.now()
    val formatter   = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    currentTime.format(formatter)
  }
}
