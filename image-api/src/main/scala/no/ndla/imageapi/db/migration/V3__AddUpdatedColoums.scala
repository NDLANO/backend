/*
 * Part of NDLA image-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.db.migration

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.DateParser
import no.ndla.common.model.NDLADate
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.Formats
import org.json4s.ext.JavaTimeSerializers
import org.json4s.native.JsonMethods._
import org.postgresql.util.PGobject
import scalikejdbc._

import java.time.LocalDateTime

class V3__AddUpdatedColoums extends BaseJavaMigration with StrictLogging {

  implicit val formats: Formats = org.json4s.DefaultFormats ++ JavaTimeSerializers.all + NDLADate.Json4sSerializer
  val timeService               = new TimeService()

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      logger.info("Starting V3__AddUpdatedColoums DB Migration")
      val dBstartMillis = System.currentTimeMillis()
      allImages.map(convertImageUpdate).foreach(update)
      logger.info(s"Done V3__AddUpdatedColoums DB Migration tok ${System.currentTimeMillis() - dBstartMillis} ms")
    }

  def allImages(implicit session: DBSession): List[V3__DBImageMetaInformation] = {
    sql"select id, metadata from imagemetadata"
      .map(rs => V3__DBImageMetaInformation(rs.long("id"), rs.string("metadata")))
      .list()
  }

  def convertImageUpdate(imageMeta: V3__DBImageMetaInformation): V3__DBImageMetaInformation = {
    val oldDocument = parse(imageMeta.document)
    val updatedJson = parse(s"""{"updatedBy": "content-import-client", "updated": "${timeService.nowAsString()}"}""")

    val mergedDoc = oldDocument merge updatedJson

    imageMeta.copy(document = compact(render(mergedDoc)))
  }

  def update(imageMeta: V3__DBImageMetaInformation)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(imageMeta.document)

    sql"update imagemetadata set metadata = $dataObject where id = ${imageMeta.id}".update()
  }

}

case class V3__DBImageMetaInformation(id: Long, document: String)

class TimeService() {

  def nowAsString(): String = {
    // NB!!! BUG day format is wrong should have been dd, and the Z should have been 'Z'
    val currentTime = LocalDateTime.now()
    DateParser.dateToString(currentTime, withMillis = false)
  }
}
