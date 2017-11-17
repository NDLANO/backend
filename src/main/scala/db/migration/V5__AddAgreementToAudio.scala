/*
 * Part of NDLA image_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package db.migration

import java.sql.Connection
import java.util.Date

import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.AudioApiProperties._
import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.json4s.native.Serialization.{read, write}
import org.postgresql.util.PGobject
import scalikejdbc._

class V5__AddAgreementToAudio extends JdbcMigration with LazyLogging  {
  // Authors are now split into three categories `creators`, `processors` and `rightsholders` as well as added agreementId and valid period
  implicit val formats = org.json4s.DefaultFormats

  override def migrate(connection: Connection): Unit = {
    val db = DB(connection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allAudios.map(t => updateAuthorFormat(t._1, t._2, t._3)).foreach(update)
    }
  }

  def allAudios(implicit session: DBSession): List[(Long, Int, String)] = {
    sql"select id, revision, document from audiodata".map(rs =>
      (rs.long("id"), rs.int("revision"), rs.string("document"))).list().apply()
  }

  def toNewAuthorType(author: V4_Author): V4_Author = {
    val creatorMap = (oldCreatorTypes zip creatorTypes).toMap.withDefaultValue(None)
    val processorMap = (oldProcessorTypes zip processorTypes).toMap.withDefaultValue(None)
    val rightsholderMap = (oldRightsholderTypes zip rightsholderTypes).toMap.withDefaultValue(None)

    (creatorMap(author.`type`.toLowerCase), processorMap(author.`type`.toLowerCase), rightsholderMap(author.`type`.toLowerCase)) match {
      case (t: String, None, None) => V4_Author(t.capitalize, author.name)
      case (None, t: String, None) => V4_Author(t.capitalize, author.name)
      case (None, None, t: String) => V4_Author(t.capitalize, author.name)
      case (_, _, _) => V4_Author(author.`type`, author.name)
    }
  }

  def updateAuthorFormat(id: Long, revision: Int, metaString: String): V5_AudioMetaInformation = {
    val meta = read[Old_AudioMetaInformation](metaString)
    val metaV5 = read[V5_AudioMetaInformation](metaString)

    // If entry contains V6 features -> Don't update.
    if(metaV5.copyright.creators.nonEmpty ||
      metaV5.copyright.processors.nonEmpty ||
      metaV5.copyright.rightsholders.nonEmpty ||
      metaV5.copyright.agreementId.nonEmpty ||
      metaV5.copyright.validFrom.nonEmpty ||
      metaV5.copyright.validTo.nonEmpty
    ) {
      metaV5.copy(id = None)
    } else {
      val creators = meta.copyright.authors.filter(a => oldCreatorTypes.contains(a.`type`.toLowerCase)).map(toNewAuthorType)
      // Filters out processor authors with old type `redaksjonelt` during import process since `redaksjonelt` exists both in processors and creators.
      val processors = meta.copyright.authors.filter(a => oldProcessorTypes.contains(a.`type`.toLowerCase)).filterNot(a => a.`type`.toLowerCase == "redaksjonelt").map(toNewAuthorType)
      val rightsholders = meta.copyright.authors.filter(a => oldRightsholderTypes.contains(a.`type`.toLowerCase)).map(toNewAuthorType)

      val x = V5_AudioMetaInformation(
        Some(id),
        Some(revision),
        meta.titles,
        meta.filePaths,
        V5_Copyright(V5_License(meta.copyright.license.license, meta.copyright.license.description, meta.copyright.license.url), meta.copyright.origin, creators, processors, rightsholders, None, None, None),
        meta.tags,
        meta.updatedBy,
        meta.updated
      )

      import java.io._
      val pw = new PrintWriter(new File(s"${x.id.getOrElse("NOP")}_NEW.json"))
      pw.write(x.toString)
      pw.close()
      val ow = new PrintWriter(new File(s"${meta.id.getOrElse("NOP")}_OLD.json"))
      ow.write(meta.toString)
      ow.close()

      x
    }
  }

  def update(audioMeta: V5_AudioMetaInformation)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(write(audioMeta))

    //sql"update audiodata set document = ${dataObject} where id = ${audioMeta.id}".update().apply
  }

}
case class V5_License( license: String, description: Option[String], url: Option[String])
case class V5_Copyright(license: V5_License, origin: String, creators: Seq[V4_Author], processors: Seq[V4_Author], rightsholders: Seq[V4_Author], agreementId: Option[Long], validFrom: Option[Date], validTo: Option[Date])
case class Old_Copyright(license: V5_License, origin: String, authors: Seq[V4_Author])
case class Old_AudioMetaInformation(id: Option[Long],
                                   revision: Option[Int],
                                   titles: Seq[V4_Title],
                                   filePaths: Seq[V4_Audio],
                                   copyright: Old_Copyright,
                                   tags: Seq[V4_Tag],
                                   updatedBy: String,
                                   updated: Date)
case class V5_AudioMetaInformation(id: Option[Long],
                                   revision: Option[Int],
                                   titles: Seq[V4_Title],
                                   filePaths: Seq[V4_Audio],
                                   copyright: V5_Copyright,
                                   tags: Seq[V4_Tag],
                                   updatedBy: String,
                                   updated: Date)

