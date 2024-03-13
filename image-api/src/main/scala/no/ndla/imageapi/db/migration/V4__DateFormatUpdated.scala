/*
 * Part of NDLA image-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.db.migration

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.DateParser
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s._
import org.json4s.ext.JavaTimeSerializers
import org.json4s.native.JsonMethods._
import org.postgresql.util.PGobject
import scalikejdbc._

import java.time.LocalDateTime

class V4__DateFormatUpdated extends BaseJavaMigration with StrictLogging {
  // There was a bug in the dateformat of V3__AddUpdatedColoums had days as DD and the 'Z' got stored as +0000 not as 'Z'.
  implicit val formats: Formats = org.json4s.DefaultFormats ++ JavaTimeSerializers.all
  val timeService               = new TimeService2()

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      logger.info("Starting V4__DateFormatUpdated DB Migration")
      val dBstartMillis = System.currentTimeMillis()
      allImages.map(fixImageUpdatestring).foreach(update)
      logger.info(s"Done V4__DateFormatUpdated DB Migration tok ${System.currentTimeMillis() - dBstartMillis} ms")
    }

  def allImages(implicit session: DBSession): List[V4__DBImageMetaInformation] = {
    sql"select id, metadata from imagemetadata"
      .map(rs => V4__DBImageMetaInformation(rs.long("id"), rs.string("metadata")))
      .list()
  }

  def fixImageUpdatestring(imageMeta: V4__DBImageMetaInformation): V4__DBImageMetaInformation = {
    val oldDocument = parse(imageMeta.document)

    val updatedDocument = oldDocument mapField {
      case ("updated", JString(oldUpdated @ _)) => ("updated", JString(timeService.nowAsString()))
      case x                                    => x
    }

    imageMeta.copy(document = compact(render(updatedDocument)))
  }

  def update(imageMeta: V4__DBImageMetaInformation)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(imageMeta.document)

    sql"update imagemetadata set metadata = $dataObject where id = ${imageMeta.id}".update()
  }

}

case class V4__DBImageMetaInformation(id: Long, document: String)

class TimeService2() {

  def nowAsString(): String = {
    val currentTime = LocalDateTime.now()
    DateParser.dateToString(currentTime, withMillis = false)
  }

}
