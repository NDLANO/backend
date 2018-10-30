/*
 * Part of NDLA audio_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package db.migration

import java.util.Date

import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.AudioApiProperties._
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.native.Serialization.{read, write}
import org.postgresql.util.PGobject
import scalikejdbc._

class V5__AddAgreementToAudio extends BaseJavaMigration with LazyLogging {
  // Authors are now split into three categories `creators`, `processors` and `rightsholders` as well as added agreementId and valid period
  implicit val formats = org.json4s.DefaultFormats

  override def migrate(context: Context): Unit = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allAudios.map(t => updateAuthorFormat(t._1, t._2, t._3)).foreach(update)
    }
  }

  def allAudios(implicit session: DBSession): List[(Long, Int, String)] = {
    sql"select id, revision, document from audiodata"
      .map(rs => (rs.long("id"), rs.int("revision"), rs.string("document")))
      .list()
      .apply()
  }

  def toNewAuthorType(author: V4_Author): V4_Author = {
    (creatorTypeMap.get(author.`type`.toLowerCase),
     processorTypeMap.get(author.`type`.toLowerCase),
     rightsholderTypeMap.get(author.`type`.toLowerCase)) match {
      case (Some(t), None, None) => V4_Author(t.capitalize, author.name)
      case (None, Some(t), None) => V4_Author(t.capitalize, author.name)
      case (None, None, Some(t)) => V4_Author(t.capitalize, author.name)
      case (_, _, _)             => V4_Author(author.`type`, author.name)
    }
  }

  def updateAuthorFormat(id: Long, revision: Int, metaString: String): V5_AudioMetaInformation = {
    val meta = read[V4_AudioMetaInformation](metaString)
    val metaV5 = read[V5_AudioMetaInformation](metaString)

    // If entry contains V6 features -> Don't update.
    if (metaV5.copyright.creators.nonEmpty ||
        metaV5.copyright.processors.nonEmpty ||
        metaV5.copyright.rightsholders.nonEmpty ||
        metaV5.copyright.agreementId.nonEmpty ||
        metaV5.copyright.validFrom.nonEmpty ||
        metaV5.copyright.validTo.nonEmpty) {
      metaV5.copy(id = None)
    } else {
      val creators =
        meta.copyright.authors.filter(a => creatorTypeMap.keySet.contains(a.`type`.toLowerCase)).map(toNewAuthorType)
      // Filters out processor authors with old type `redaksjonelt` during import process since `redaksjonelt` exists both in processors and creators.
      val processors = meta.copyright.authors
        .filter(a => processorTypeMap.keySet.contains(a.`type`.toLowerCase))
        .filterNot(a => a.`type`.toLowerCase == "redaksjonelt")
        .map(toNewAuthorType)
      val rightsholders =
        meta.copyright.authors
          .filter(a => rightsholderTypeMap.keySet.contains(a.`type`.toLowerCase))
          .map(toNewAuthorType)

      V5_AudioMetaInformation(
        Some(id),
        Some(revision),
        meta.titles,
        meta.filePaths,
        V5_Copyright(meta.copyright.license,
                     meta.copyright.origin,
                     creators,
                     processors,
                     rightsholders,
                     None,
                     None,
                     None),
        meta.tags,
        meta.updatedBy,
        meta.updated
      )
    }
  }

  def update(audioMeta: V5_AudioMetaInformation)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(write(audioMeta))

    sql"update audiodata set document = ${dataObject} where id = ${audioMeta.id}".update().apply
  }

}
case class V5_Copyright(license: String,
                        origin: Option[String],
                        creators: Seq[V4_Author],
                        processors: Seq[V4_Author],
                        rightsholders: Seq[V4_Author],
                        agreementId: Option[Long],
                        validFrom: Option[Date],
                        validTo: Option[Date])
case class V5_AudioMetaInformation(id: Option[Long],
                                   revision: Option[Int],
                                   titles: Seq[V4_Title],
                                   filePaths: Seq[V4_Audio],
                                   copyright: V5_Copyright,
                                   tags: Seq[V4_Tag],
                                   updatedBy: String,
                                   updated: Date)
