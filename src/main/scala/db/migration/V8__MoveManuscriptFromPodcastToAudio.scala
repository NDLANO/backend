/*
 * Part of NDLA audio-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.JsonAST.{JArray, JField, JString}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.{DefaultFormats, Extraction, JObject}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V8__MoveManuscriptFromPodcastToAudio extends BaseJavaMigration {
  case class V7__CoverPhoto(imageId: String, altText: String)
  case class V7__PodcastMeta(header: String,
                             introduction: String,
                             coverPhoto: V7__CoverPhoto,
                             manuscript: String,
                             language: String)
  case class V8__Manuscripts(manuscript: String, language: String)
  case class V8__PodcastMeta(header: String, introduction: String, coverPhoto: V7__CoverPhoto, language: String)

  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats

  override def migrate(context: Context): Unit = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allAudios.map {
        case (id: Long, document: String) => update(convertDocument(document), id)
      }
    }
  }

  def allAudios(implicit session: DBSession): List[(Long, String)] = {
    sql"select id, document from audiodata"
      .map(rs => (rs.long("id"), rs.string("document")))
      .list()
      .apply()
  }

  def convertDocument(document: String): String = {
    val oldArticle = parse(document)
    val oldPodcastMetas = (oldArticle \ "podcastMeta").extract[Seq[V7__PodcastMeta]]

    val newManuscripts = oldPodcastMetas.map(
      meta =>
        V8__Manuscripts(
          meta.manuscript,
          meta.language
      ))
    val newPodcastMetas = oldPodcastMetas.map(
      meta =>
        V8__PodcastMeta(
          meta.header,
          meta.introduction,
          meta.coverPhoto,
          meta.language
      ))

    val newArticle = oldArticle
      .replace(List("podcastMeta"), Extraction.decompose(newPodcastMetas))
      .merge(JObject(JField("manuscript", Extraction.decompose(newManuscripts))))

    compact(render(newArticle))
  }

  def update(document: String, id: Long)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update audiodata set document = ${dataObject} where id = $id".update().apply()
  }

}
