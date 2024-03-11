/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.JsonAST.JField
import org.json4s.ext.{EnumNameSerializer, EnumSerializer}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.*
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V10__AudioTypeFromNumberToString extends BaseJavaMigration {

  object MigrationAudioType extends Enumeration {
    val Standard: this.Value = Value("standard")
    val Podcast: this.Value  = Value("podcast")
  }

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      allAudios.map { case (id: Long, document: String) =>
        update(convertDocument(document), id)
      }
    }: Unit

  def allAudios(implicit session: DBSession): List[(Long, String)] = {
    sql"select id, document from audiodata"
      .map(rs => (rs.long("id"), rs.string("document")))
      .list()
  }

  def convertDocument(document: String): String = {
    val enumManifest = manifest[MigrationAudioType.Value]

    val oldFormats = DefaultFormats + new EnumSerializer(MigrationAudioType)
    val newFormats = DefaultFormats + new EnumNameSerializer(MigrationAudioType)

    val oldArticle = parse(document)

    val existingAudioType = (oldArticle \ "audioType").extractOpt[MigrationAudioType.Value](oldFormats, enumManifest)
    val audioType         = existingAudioType.getOrElse(MigrationAudioType.Standard)
    val audioTypeString   = Extraction.decompose(audioType)(newFormats)

    val objectToMerge = JObject(JField("audioType", audioTypeString))
    val newArticle    = oldArticle.merge(objectToMerge)

    compact(render(newArticle))
  }

  def update(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update audiodata set document = ${dataObject} where id = $id".update()
  }

}
