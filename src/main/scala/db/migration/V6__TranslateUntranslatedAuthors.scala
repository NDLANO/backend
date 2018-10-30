/*
 * Part of NDLA audio_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package db.migration

import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.AudioApiProperties._
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.native.Serialization.{read, write}
import org.postgresql.util.PGobject
import scalikejdbc._

class V6__TranslateUntranslatedAuthors extends BaseJavaMigration with LazyLogging {
  // Translates authors that wasn't translated in V5
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

  private def toNewAuthorType(author: V4_Author): V4_Author = {
    (creatorTypeMap.getOrElse(author.`type`.toLowerCase, None),
     processorTypeMap.getOrElse(author.`type`.toLowerCase, None),
     rightsholderTypeMap.getOrElse(author.`type`.toLowerCase, None)) match {
      case (t: String, _, _) => V4_Author(t.capitalize, author.name)
      case (_, t: String, _) => V4_Author(t.capitalize, author.name)
      case (_, _, t: String) => V4_Author(t.capitalize, author.name)
      case (_, _, _)         => author
    }
  }

  def updateAuthorFormat(id: Long, revision: Int, metaString: String): V5_AudioMetaInformation = {
    val meta = read[V5_AudioMetaInformation](metaString)

    val creators = meta.copyright.creators.map(toNewAuthorType)
    val processors = meta.copyright.processors.map(toNewAuthorType)
    val rightsholders = meta.copyright.rightsholders.map(toNewAuthorType)

    meta.copy(id = Some(id),
              revision = Some(revision),
              copyright =
                meta.copyright.copy(creators = creators, processors = processors, rightsholders = rightsholders))
  }

  def update(audioMeta: V5_AudioMetaInformation)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(write(audioMeta))

    sql"update audiodata set document = ${dataObject} where id = ${audioMeta.id}".update().apply
  }

}
